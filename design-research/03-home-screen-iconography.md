# 03 — Home-Screen Design and Iconography

**Project:** Marlow Launcher — a Pixel-style Android home launcher, package `app.launcher`, Compose + Material 3.
**Purpose:** Research only. No code was changed for this document.
**Method:** Public sources only — Android Developers documentation, Material Design 3 (m3.material.io), Apple Human Interface Guidelines and Apple Support, Google Support, and published design writing. Every factual claim below is tied to a cited source in the Sources list at the end. Where a number is a design recommendation derived from several sources rather than a single official specification, it is labelled as such. No employment, education, or insider claim is made anywhere in this document; the research stands on the cited sources alone.
**Date:** 2026-11-26.

---

## 1. Principles first

### 1.1 The home screen is a glance-and-launch surface, not a dashboard

A home screen has three jobs, in this order: (1) answer "what do I need to know right now?" without opening an app, (2) launch the handful of apps used every day in one tap, (3) get out of the way. Google's At a Glance documentation frames the top strip exactly that way — future events, tasks, and weather on the home or lock screen — and Android's widget overview frames widgets as teasers that "consolidate and concentrate an app's information and provide a connection to richer detail within the app," with the app itself always showing more detail than the widget. A home screen that tries to be a full dashboard — every widget, every shortcut, every feed — fails at job (1) because nothing is glanceable any more.

The practical test, from home-screen layout writing, is to ask of every item on page one: do I need to *see* this immediately, *launch* this immediately, or *act* on this immediately? If it answers none of the three, it belongs in a folder, on a secondary page, or in All Apps. Removing an icon from the home screen is not uninstalling it; Android separates home-screen shortcuts from the complete app list.

### 1.2 Visual hierarchy: one thing first, one thing second, everything else quiet

On a Pixel-style home, the eye should hit, in order:

1. **The glanceable strip at the top** — date and weather (At a Glance). It is the only element that changes through the day without the user doing anything, so it earns the top.
2. **The dock at the bottom** — the stable, always-there row. It is the second thing the eye finds because it never moves between pages, and the thumb already lives there.
3. **The app grid in the middle** — the working area. It should read as a calm, even field, not as a set of competing posters.

Material Design's hierarchy tools are elevation, size, colour, and space — in that order of strength on a home screen, space does most of the work. Material's spacing guidance is explicit that spacing is measured on an 8dp scale (`space100 = 8dp`), that padding and gaps belong on the parent container, and that margins on child elements are a last resort because they are rarely uniform. Whitespace is not empty space left over after placing things; it is the tool that creates focus. Print-derived Material writing puts it bluntly: whitespace is the most effective tool for creating focus and grabbing attention, and beginners most often fail by fearing blank space.

Consequences for Marlow: the top strip and the dock should be the only elements with a distinct visual treatment (a pill, a scrim, a container). The grid itself should have no boxes, no dividers, no per-icon backgrounds. If the grid needs a box to look organised, the spacing is wrong.

### 1.3 Density vs. calm: the grid is a rhythm, not a packing problem

A 5-column Pixel-style grid (5×5 on most phones, 4×5 on smaller ones, per the Pixel-replication guidance) holds a lot — but "holds a lot" is not the goal. The stock feeling comes from *even* rhythm: identical cell sizes, identical icon sizes, identical gaps, identical label baselines, generous edge padding, and a visible amount of unused space on page one. The Pixel-replication guidance is specific: modest icon size, generous spacing, a mostly empty first page, medium/large horizontal padding, medium vertical padding, and no flashy scroll effects.

Density is a dial, not a virtue. A denser grid (6 columns, smaller icons) is legitimate as a user option, but it must keep the same rhythm rules — one icon size, one gap, one label style — or it reads as clutter rather than density. The amateur tell is not "many icons" but "uneven icons": different sizes, different gaps, labels wrapping at different heights, one widget that does not align to the grid.

### 1.4 The wallpaper is the background, never a competitor

Labels and icons sit on a background the launcher does not control — the user's photo. Contrast is therefore a relationship the launcher only half-controls. The correct mental model, from writing on text over images, is: find the *worst* pixel behind the text, not the average, and either darken that patch with a scrim or choose a label treatment that survives it. A flat scrim at 30–50% black is usually enough to make white text hold on anything short of pure white; a gradient scrim (transparent at one end, 50–70% black where the text sits) keeps the image vivid where nothing needs reading and goes quiet exactly where something does. The common implementation mistake is a plain two-stop linear gradient: the eye spots the line where the gradient begins, so intermediate stops must ease the darkening in. A scrim is also the only approach that keeps working when the user swaps the wallpaper — a solid overlay with a known opacity gives a known range of background values, while a photograph on its own gives a range nobody controls.

Material's contrast floor is 4.5:1 for normal text and 3:1 for large text. On a home screen that floor has to hold against the *lightest* patch behind a white label (or the darkest patch behind a dark label), not against a flat swatch in a design tool.

### 1.5 Icons are a system, not a collection

Material Design 3's icon principles are: simplify for clarity and legibility; don't be overly literal or complex; make icons graphic and bold; don't use delicate or loose organic shapes; use and maintain one consistent visual style throughout one icon set; never mix styles in one set. The system-icon rules make that concrete: standard size 24dp × 24dp, content inside a 20dp × 20dp live area with 2dp padding, 2dp corner radius by default, 2dp (regular / 400) stroke weight, squared stroke terminals, consistent stroke weights, icons facing forward — never tilted, rotated, or dimensional.

Launcher (app) icons are a different scale from system icons, but the same discipline applies at the launcher's scale: one mask shape for every icon on the device, one apparent size, one corner-radius logic, one colour discipline. Android's adaptive-icon system exists precisely to enforce that uniformity across apps the launcher author never drew: the OEM supplies one mask, the system renders every adaptive icon with the same shape.

### 1.6 The stock feeling is restraint

Pixel Launcher's appeal, as described by people who keep returning to it after trying third-party launchers, is that it is boring and reliable: a simple home screen, a useful glanceable area, quick access to search, a tidy drawer, smooth navigation — "a good ol' boring, reliable setup that never fails and always gets me to the app I need in one tap, two taps, or two taps and one swipe, max." The stock feeling is not a particular shade of blue. It is: nothing shouts, nothing is half-finished, nothing asks the user to make a decision before they have launched a single app. Every extra choice on the surface — a second search bar, a third widget, a settings shortcut, a tip card — subtracts from that feeling.

---

## 2. Home-screen layout

### 2.1 What belongs on page one, and where

There is no Google-published "first-boot must contain exactly N apps" specification. The layout below is a synthesis of the Pixel conventions documented in the sources (At a Glance at the top; a small set of favourite apps in the middle; a 5-icon dock; a search entry point) and the functional-layer model from home-screen layout writing (Glance layer / Action layer / Stable-access layer). Treat the counts as the Marlow default to test, not as a platform mandate.

| Zone | Contents (default) | Why it belongs there |
|---|---|---|
| Top | One glanceable strip: date + weather (At a Glance) | Answers a question without a launch; changes through the day; earns the top because it is the only live element |
| Upper-middle | One widget at most on first boot — clock or weather, not both, and never a duplicate of what At a Glance already shows | A widget earns space only if it is read often; a second information widget showing the same facts is duplication, not usefulness |
| Middle | 8–12 apps: the daily set, arranged by action/frequency, not alphabetically and not by colour | These are the actions to start with minimal searching; 4–8 is the "most-used" core in the layout literature, 8–12 is the full first-page set once folders are counted as single cells |
| Bottom | Dock: 5 apps, stable across all pages — Phone, Messages, browser, mail, Camera (or the user's own daily five) | The dock remains available while moving between pages; it is for stable daily actions, not for whatever was opened last |
| Bottom edge / below dock | One search entry point (Pixel search bar or a search pill) | Search is the escape hatch for everything not on page one; it should be reachable by the thumb without scrolling |

What page one must *not* carry on first boot: a second page of apps the user never asked for, an empty page, a "tips" card, a settings shortcut, a store shortcut, or more than two information widgets. An empty first boot is the single most common amateur failure: it tells the user the launcher has nothing to offer and forces them to build the home screen before they can use the phone.

### 2.2 Dock composition

- **Five icons.** The Pixel-replication guidance sets dock icons to 5, one dock page, dock icon size matching desktop icons (100–110%). The long-term Pixel-user account in the sources uses five dock icons (Phone, WhatsApp, Camera, Chrome, Spotify) with five folders above them.
- **One row, one page.** No second dock page, no paged dock, no dock widgets. A two-row dock or a dock widget breaks the "stable access" contract: the thumb expects the same five targets in the same places every time.
- **Same size as grid icons, same mask, same label treatment as the grid** (Pixel shows labels on desktop icons; the dock in Pixel Launcher shows no labels under dock icons — the dock's job is muscle memory, and its icons are the five most familiar on the phone). Whichever Marlow chooses, it must be one rule for all five, not a mix.
- **Kinds, not just names:** one calling/messaging pair (Phone + Messages, or the user's messenger), one browser, one mail, one camera. A dock of five social apps or five games is a category error — the dock is for the actions that are needed from *every* page, including inside other apps' contexts.
- **No app-drawer button in the dock.** Pixel Launcher removed the drawer button; the drawer opens by swiping up. A drawer button in the dock costs one of the five stable slots and duplicates a gesture every Pixel user already knows.

### 2.3 Clock and weather placement

- Clock and weather belong in the glanceable strip, not as two separate large widgets competing for the top. At a Glance's documented content is future events, tasks, and weather; the full experience is on Pixel devices with Android 12 and up, and its categories can be individually turned on or off, including sensitive content on the lock screen.
- If Marlow ships its own clock/weather widget instead of At a Glance, it must occupy the same top zone and the same width as At a Glance would (full-width, one row tall), so the rest of the grid does not shift when the user swaps providers.
- Never show the time in three places at once (status bar + At a Glance + clock widget). Duplication is the clearest signal of an unedited home screen.
- Weather belongs next to the date, in the same line or the same strip, at a smaller size than the date — it is supporting information, not a headline.

### 2.4 What At a Glance should and shouldn't carry

Should carry: next calendar event (with time), today's weather (condition + high/low), a task or reminder only if it is due today, a travel/pass/alert item only when it is actually imminent. The pattern is *contextual and sparse*: the strip changes because the day changes, and most of the time it shows very little.

Shouldn't carry: a news headline feed, a persistent "good morning" greeting, a promotional card, more than one event at a time, a full agenda list, a battery percentage, or any item that does not expire. A strip that always shows the same five things is not "at a glance" — it is a widget that never changes, and the user will stop reading it. Google provides per-category on/off controls for At a Glance; Marlow should expose the same granularity rather than a single on/off switch.

Privacy corollary: At a Glance is personalised and may use calendar permissions, app information, account activity, location, and nearby-device access. Sensitive content on the lock screen must be separately controllable, as Google documents. A launcher that surfaces a calendar event on the lock screen without that control has shipped a privacy bug, not a feature.

### 2.5 Widget sizing and placement

Android's widget guidance is structural: widgets fall into information, collection, control, or hybrid types; only touch and vertical swipe are available as gestures inside a widget (no horizontal swipe, no long-press drag inside the widget, because those gestures belong to the home screen); a widget must define `minWidth`/`minHeight` and will be stretched to occupy the minimum number of grid cells that satisfy them; well-designed widgets leave margins between the bounding box and the frame, and padding between the frame and the controls, so they align with other widgets and icons.

Placement rules that follow from that:

- **Widgets align to the grid.** A widget that is half a cell off the grid, or whose internal padding does not match the icon grid's rhythm, will look broken next to icons even if its own design is good. Resize handles must snap to whole cells.
- **Size follows the job, not the other way round.** A 2×2 widget is for one number (battery, temperature, days left). A 4×2 widget is for a short label plus a bar or two lines. A 4×4 widget is for a calendar or a photo. A widget that is large but shows one line of text is wasting the most expensive space on the phone.
- **One widget per job.** Two weather widgets, or a weather widget plus At a Glance weather, is duplication. If the user adds a second one, that is their choice; the *default* must never duplicate.
- **Widgets are teasers.** The widget shows less than the app; tapping an information element goes to the detail screen, and there should be a path to the app's top level. A widget that tries to be the whole app (full lists, full controls, scrolling feeds) will fight the home screen's gestures and lose.
- **Resizable, but within the grid.** Users can move widgets across panels and, if supported, resize them. Marlow must support resize to whole-cell sizes and must reflow the widget's content at each size rather than stretching or cropping it.

### 2.6 Folders: look and behaviour

- **Creation:** drag one app on top of another. That gesture is documented identically for Pixel Launcher and for iOS, and users on both platforms expect it. There must be no other primary way to create a folder (a "create folder" button in a menu is fine as a secondary path; a folder that can only be created from Settings is not).
- **Closed-folder preview:** the Pixel/Android N convention is a circular "window" through which the contained apps are visible as clipped mini-icons (up to four visible in the preview), replacing the older stacked-icons look. The preview must show the *first* apps in the folder in a stable order, so the user learns to recognise a folder by its preview.
- **Open behaviour:** tapping a folder opens it as a sheet/grid showing all contained apps at full icon size, with the folder name editable at the top (tap the name to rename). Tapping empty space outside the open folder closes it. The open folder must not push the rest of the home screen around.
- **Naming:** folder names are short, user-chosen, task-based ("Work", "Google", "Media", "Travel") — not auto-generated category labels the user cannot change. An unnamed folder ("Folder") on first boot is unfinished work.
- **Folder as a cell:** a folder occupies exactly one grid cell, with the same size, mask treatment, and label position as an app icon. A folder that is larger, smaller, or differently shaped than its neighbours breaks the grid rhythm.
- **Count discipline:** on a stock-feeling first boot, folders are for the *second* tier — the apps used weekly, not daily. The long-term Pixel-user account in the sources uses five folders above the dock for everything not in the dock, and never opens the drawer. That is a legitimate end-state, but it is the *user's* end-state after organising; Marlow's first boot should ship at most one or two folders (e.g. "Google"), not five pre-filled ones the user did not choose.

### 2.7 Balance: density and calm

- Leave visible empty space on page one. A first page that is full to the last cell reads as a storage shelf, not a home screen.
- One visual idea per page. If page one has a photo widget, it should not also have a brightly coloured widget and a folder full of games. The wallpaper, the widget, and the icons are three voices; they must not all sing at once.
- The grid must survive the user's worst wallpaper. Test the default layout against a bright photo, a dark photo, and a busy photo; if any of them makes the grid unreadable, the fault is the label/scrim treatment, not the wallpaper.

---

## 3. Iconography

### 3.1 Adaptive icons: layers, safe zone, masks

This is the one section where the platform documentation is exact, and Marlow must implement it exactly — both for its own icon and for how it renders third-party icons.

- **Two layers for the colour version, plus one optional monochrome layer.** Foreground (the mark) and background (the field behind it), each 108×108 dp; vectors preferred over bitmaps. The monochrome layer is a single layer for the themed version. (`android:icon` in the manifest; `android:roundIcon` is the optional circular variant.)
- **Safe zone: 66×66 dp, centred.** The 66×66 dp safe zone is the area never clipped by any OEM-shaped mask. The logo must be at least 48×48 dp and must not exceed 66×66 dp, because the inner 66×66 dp is what appears within the masked viewport. The outer 18 dp on each side is reserved for masking and for visual effects such as parallax or pulsing.
- **Masks vary by device.** The same icon is a circle on one OEM device and a squircle on another; each OEM must provide a mask, and the system renders all adaptive icons with the same shape. Marlow, as a launcher, is the party that *applies* the mask — so Marlow must apply one mask, consistently, to every adaptive icon it shows. It must never show a circle for one app and a squircle for another because of how the icon was packaged.
- **Clean edges.** Layers must not have masks or background shadows baked around the outline of the icon. The launcher supplies the shadow and the mask; a baked-in shadow reads as a second, wrong shadow, and a baked-in mask produces a doubly-rounded corner.
- **Background layer discipline.** The background fills the whole 108 dp canvas (solid colour, gradient, or pattern) and must not contain the primary logomark or text — the mark belongs in the foreground, where parallax applies. A background that contains a second copy of the logo will parallax against itself.
- **Legacy icons.** Apps without an adaptive icon still need to look like they belong. The launcher must place a legacy icon on a consistent tray (the documented One UI-style approach uses a light tray at a fixed scale, e.g. 0.72, with a legacy crop/contain rule) rather than letting each legacy icon keep its own baked shape and size. A home screen where half the icons are masked adaptive icons and half are raw legacy squares is the fastest way to look amateur.
- **Store icon is separate.** The Google Play listing icon is a 512×512 px 32-bit PNG, no transparency, full square — Google applies a 30% corner radius itself. It is not generated from the adaptive XML, and the launcher icon must not be judged by how the store icon looks, or vice versa.

### 3.2 Icon consistency across third-party icons

The launcher cannot redraw third-party icons, but it controls everything around them, and that is where consistency is won or lost:

- **One apparent size.** Different apps ship logos at different sizes inside the 48–66 dp range. Marlow should normalise the *rendered* icon size so that a small-logo app and a large-logo app do not look like different sizes on the same grid. The documented recommendation for a logomark is roughly 54–60 dp within the 108 dp canvas to keep visual weight consistent with system icons; Marlow's rendering should aim for that band.
- **One mask, one corner radius, one shadow.** Every icon gets the same mask shape, the same corner-radius logic, and the same drop shadow (or none). No per-app exceptions.
- **Colour discipline is the launcher's job too.** A grid of twenty fully saturated, unrelated brand colours is inherently noisy; the launcher cannot desaturate other apps' icons, but it can refuse to add more colour of its own — no coloured icon backgrounds, no coloured folder tints, no coloured label chips. The grid's colour should come from the icons themselves, and everything the launcher draws (folder backgrounds, search pill, dock) should be neutral or drawn from the Material You palette.
- **System icons (Marlow's own UI icons) follow M3, not the app-icon rules.** 24dp grid, 20dp live area, 2dp stroke, 2dp corner radius, one style (outlined *or* filled *or* rounded — never mixed in one set), stroke weight matched to adjacent text weight. A settings screen that mixes outlined, filled, and rounded system icons looks unconsidered even if each icon is individually fine.

### 3.3 Themed / monochrome icons (Material You)

- **How they work.** From Android 13 (API 33), if the user enables themed app icons and the launcher supports it, the system tints the app's `monochrome` layer using the wallpaper/theme-derived palette (typically the primary-container colour). From Android 16 QPR 2, Android automatically themes app icons for apps that don't provide their own monochrome layer. If the user has not enabled themed icons, the app has no monochrome layer (and the device is below Android 16 QPR 2), or the launcher does not support themed icons, the home screen shows the adaptive or standard icon instead.
- **What breaks them.** The monochrome layer is a solid-alpha silhouette of the primary mark only. Anything that relies on internal colour contrast — two-tone logos, gradients, drop shadows, thin detail, anti-aliased shadow — collapses when a single tint is multiplied against the alpha channel. Compact marks with filled interiors (a dark blob) lose legibility when tinted against a similarly-toned background. The themed-icon safe zone is in practice smaller than the 66 dp adaptive safe zone (secondary guidance puts it at roughly 36–60 dp); a mark that fills the full 66 dp in colour will feel cramped and muddy when themed.
- **Mixed grids are normal and must be designed for.** Unsupported apps keep their normal icons, so a themed home screen is a mixture of themed and colourful icons. Marlow must not "fix" this by forcing a tint onto apps without a monochrome layer (below Android 16 QPR 2) or by desaturating them — the mixture is the documented behaviour, and a launcher that invents its own theming for those apps will produce worse results than the system's auto-theming on Android 16 QPR 2.
- **Test against a range of tints.** The tint is drawn from the user's wallpaper-derived palette — warm orange, cool blue, desaturated green, neutral grey are all possible. A monochrome layer (Marlow's own icon) must be tested against light and dark tints and light and dark backgrounds, not just the developer's own wallpaper.
- **Marlow's own icon must ship a monochrome layer.** On Android 13+ with Material You enabled, launchers display themed icons in a unified grid; an app without a monochrome layer is the one colourful square in a monochrome grid, which reads as a bug.

### 3.4 Icon label typography

- **Size.** Material's type scale gives `labelMedium` 12sp / Medium (500) and `labelSmall` 11sp / Medium (500), with `bodySmall` 12sp / Regular. Home-screen app labels sit in that band: 12sp is the working size, 11sp is the floor for a label, and nothing on the home screen should be set below 11sp. Body text must never go below 12sp. All text uses `sp`, never `dp`, so it follows the user's font scaling.
- **One line, truncated with an ellipsis — never two lines, never clipped.** A label that wraps to two lines makes its cell taller than its neighbours and breaks the row rhythm; a label that is clipped mid-glyph without an ellipsis looks like a rendering bug. The long-app-name case (the Android Central account of a Pixel 3a where "Google P…" ended in ellipses) is the visible symptom of a grid that is too narrow for its labels — the fix is the grid/label-size pairing, not hiding the ellipsis.
- **When labels are omitted.** Labels may be omitted only where recognition does not depend on them: the dock (five ultra-familiar apps, muscle memory), and an explicit user-chosen "large icons, no labels" mode (the iOS 18 pattern — see §7). Labels must never be omitted on the main grid by default: on the grid, the label is what distinguishes two similar-looking icons, and it is the accessible name for the app. If Marlow offers a hide-labels option, it must be opt-in and must not change the cell height (the space the label occupied becomes breathing room, not a layout shift).
- **Weight and colour.** Labels are one weight (Regular or Medium — pick one, use it everywhere) and one colour treatment for the whole grid. A label that is bold for one app and regular for another is a hierarchy claim the launcher has no basis for making.
- **Scaling.** Text must survive 200% font scaling without clipping or overlapping. That means label cells must be measured with scaled text in mind, and the truncation rule must still hold at the largest scale — if a label cannot fit at 200% scale, it truncates; it must never overlap the icon below it.

### 3.5 Notification badges / dots

Android's model and the general badge-design rules pull in slightly different directions, and Marlow must honour both:

- **Android's model is a dot, not a number.** From Android 8.0 (API 26), notification badges — "notification dots" — appear on a launcher icon when the associated app has an active notification. They appear by default in launchers that support them; the app does nothing. Long-pressing the icon reveals the notifications (and app shortcuts). The count shown in the long-press menu is the number of active notifications, customisable per notification via `setNumber()`; a channel can opt out entirely via `setShowBadge(false)`. There is no Android-native API for a launcher to set an arbitrary badge number independent of notifications.
- **Placement.** The dot sits pinned to the top corner of the icon (top-right in LTR), overlapping the icon edge, without moving the underlying icon. It must never shift the icon, the label, or the neighbouring cells. Its position must be identical for every icon, regardless of the icon's shape or mask.
- **Size.** Small relative to the icon — a dot, not a pill. It must be legible at a glance but must not cover the recognisable part of the icon. A badge that covers a quarter of the icon is covering the brand it is annotating.
- **Colour.** One colour for all badges (the system/error red family on both platforms — iOS badges are always red; Android dots use the app's notification colour by default, but a launcher that lets every app pick its own dot colour has shipped confetti). The dot's meaning is "something needs you"; if it is also used for "something happened, no action needed", the meaning is diluted and users learn to ignore it.
- **Dot vs. count.** A dot means "there is something new"; a number means "there are N things". Never show both on the same icon, and never show a number where a dot would do — badge-design guidance is consistent: cap numeric counts at 99+, keep the badge shape consistent so the layout does not reflow as the count grows, clear the badge immediately when the associated view is opened, and reserve badging for items that require a user decision.
- **Per-app control.** The user must be able to turn badges off per app (Android: per-channel `setShowBadge(false)` is the app's side; the launcher's side is a per-app badge setting), and Marlow must respect the system's badge settings rather than inventing its own count from notification-listener data that disagrees with the system's dot.

---

## 4. Legibility on a home screen

### 4.1 Label contrast over wallpapers

- **The 4.5:1 floor applies to labels.** Normal text needs 4.5:1 against its background; large text (18sp+, or 14sp+ bold) needs 3:1; non-text UI components need 3:1. A home-screen label is normal text, so 4.5:1 is the floor — and the "background" is the worst patch of wallpaper behind that label, not the average brightness of the whole wallpaper.
- **Test the worst case, not the average.** A photo can average a comfortable mid-grey while the exact patch behind one word is nearly white. The three practical ways to find the worst patch are: look at the image at display size and eyedropper the least-separated area; blur the image heavily so bright regions show as blobs; or stop guessing and add a scrim, which turns the variable background into a controlled one.
- **Choose the label colour from the wallpaper, or fix the wallpaper's luminance under the labels — don't do neither.** The two honest approaches are: (a) read the wallpaper's luminance (e.g. via `WallpaperColors` — the dark-text hint and the colour clusters) and pick a dark or light label colour per wallpaper, or (b) lay a scrim under the label zone so one label colour always passes. Shipping white labels on an uncontrolled wallpaper with no scrim and no luminance check is shipping unreadable text on some user's phone.
- **Text shadow is a supplement, not a solution.** A soft shadow behind a label lifts it off a busy patch, and it is the lightest-weight fix. But a shadow alone, with no scrim and no luminance check, will still fail on a bright patch — the shadow is dark, the patch is bright, and the letterforms in between are still low-contrast. Use shadow *plus* scrim or luminance-aware colour, and measure the result.
- **Measure, don't eyeball.** The legibility check is a number: the contrast ratio of the label colour against the lightest pixel in the label's band (for a light label) on each of a set of test wallpapers (bright, dark, busy, photographic, flat). The Willow Launcher commit history — a real launcher project working through exactly this problem — records the pattern: a fixed 36% scrim read as a black column on a dark wallpaper, a 22% top-down dim left the clock at 2.96:1 on a pale wallpaper, and a luminance-aware dim (42% black at the top when any wallpaper cluster reads light, eased out by half the page) put the clock at 4.99:1 and the date at 6.51:1. The numbers, not the intention, are the test.

### 4.2 Scrim / dim rules

- **Flat scrim:** 30–50% black under white text is usually enough on anything short of pure white. Use it where text sits over the whole image (e.g. a full-screen folder sheet over a wallpaper).
- **Gradient scrim:** transparent at one end, 50–70% black where the text sits; ease it with intermediate stops so there is no visible band where the darkening begins. Use it where text lives in one region (the top strip, the dock zone, the bottom of a widget).
- **Scale the scrim to the ground behind it.** A fixed scrim that is right on a light wallpaper is wrong on a dark one (it reads as a black bar). The scrim's strength should follow the wallpaper's brightness: strong over bright art, fading to nothing over dark art, with a faint veil (around 8%) as the fallback when no wallpaper pixels can be read.
- **No hard edges.** A scrim must never have a visible edge, seam, or shoulder. If a screenshot shows a line where the scrim starts or ends, the easing is wrong. Test scrims on a smooth light wallpaper, where banding is most visible.
- **Scrims are for legibility, not for decoration.** A scrim that is visible as a design element — a dark panel, a coloured wash, a "glass" bar that draws the eye — has stopped being a scrim and become a surface. If the user can see the scrim, it is too strong or too sharply bounded.

### 4.3 Icon spacing rhythm

- **One gap, everywhere.** Horizontal and vertical gaps between cells are a single token (on the 8dp scale — 8dp is the base unit; the gap between home-screen cells is a small multiple of it). The gap between an icon and its label is a separate, smaller token, and it is the same for every cell.
- **Edge padding is larger than the inter-cell gap.** Content must breathe at the screen edges; the Pixel guidance calls for medium/large horizontal padding and medium vertical padding. If the outer icons touch the screen edge, the grid reads as overflowing rather than composed.
- **The dock is separated from the grid by more than one gap.** The dock is a distinct zone; the space above it should be visibly larger than the gap between two rows of icons, so the eye parses "grid, then dock" rather than "six rows, the last of which is different for no reason".
- **Vertical rhythm is set by the label, not the icon.** Every cell has the same height — icon + fixed label slot — whether or not that particular app's label is short, long, or truncated. A cell whose label is missing must keep the same height (empty label slot), or the row below it will sit at a different baseline.
- **Alignment is to the grid, not to the wallpaper.** Widgets, icons, folders, and the search pill all align to the same column lines. A widget that is inset by a different amount than the icons is misaligned even if it looks "about right".

---

## 5. Common amateur mistakes in launcher home screens

Each of these is a failure mode seen repeatedly in third-party launchers and in first-boot defaults. They are listed here so Marlow can be checked against them directly.

1. **Empty first boot.** A home screen with a wallpaper and nothing else — no apps, no dock, no widget — on first launch. The user has to build the launcher before using the phone. (Fix: curated first-run seeding — page 0, dock, clock/weather on by default.)
2. **Inconsistent icon sizes.** Some icons rendered larger than others because their logos were drawn at different sizes inside the safe zone, or because legacy icons were not normalised onto a tray. The grid looks like a rummage sale.
3. **Labels clipping or wrapping.** Labels cut off mid-glyph, or wrapping to two lines and pushing the row below down. The fix is one line + ellipsis + a grid wide enough for the label size, tested at 200% font scale.
4. **Too many choices on the surface.** A tips card, a settings shortcut, a second search bar, three widgets, and a feed preview on page one. Every item that is not glance/launch/act is a decision the user did not ask to make.
5. **Wallpaper fighting the icons.** A busy photo with white labels and no scrim, or a bright photo with a dark dock pill that disappears into it. The wallpaper is the background; if it wins the contrast contest, the launcher has failed.
6. **Two sources of truth for the same fact.** Weather in At a Glance *and* in a weather widget *and* in a clock widget; the time in the status bar *and* in a clock widget. Duplication reads as indecision.
7. **A dock that changes.** Suggested apps in the dock, a rotating dock, or a dock that gains and loses icons. The dock's entire value is stability; a dock that changes is a second grid, not a dock.
8. **Folders that hide the apps.** A folder preview that shows a generic folder glyph instead of the contained apps, or a folder that opens to a list instead of a grid of full-size icons. The user should be able to recognise a folder by its contents at a glance.
9. **Badges everywhere.** A red dot on every icon, or a numeric badge on apps with no actionable unread state. Badging is a scarce signal; spending it on everything spends it on nothing.
10. **Themed icons applied by force.** A launcher that tints every icon with a single colour filter (instead of using each app's monochrome layer, or the system's auto-theming on Android 16 QPR 2) produces muddy, low-contrast icons for apps whose marks were never designed as silhouettes.
11. **A grid that doesn't align.** Widgets half a cell off, a search pill inset differently from the icons, a dock with different horizontal padding from the grid. Misalignment is visible at a glance even to users who could not name it.
12. **Motion that draws attention to itself.** Cube/card-stack/zoom page transitions, bouncing icons, parallax that is visible rather than felt. Pixel Launcher is intentionally restrained; the scroll effect should be Simple or None.
13. **Settings strings that leak the plan.** Developer asides, feature numbers, and internal names in user-visible strings ("#134", "Phase 2", "experimental") — the user should never see the project's internal organisation chart.
14. **No empty states, no error states.** A widget that shows a blank rectangle when it has no data, a folder that shows nothing when it is empty, a search bar that does nothing when tapped. Every surface needs a designed empty/error state, or it reads as broken.

---

## 6. What "stock" means, concretely

If Marlow is checked against one paragraph, it is this: on first boot, a user who has only ever used a Pixel should feel at home within five seconds — At a Glance at the top with today's date and weather, a calm grid of familiar apps in the middle with readable one-line labels, five stable apps in the dock, a search entry point at the bottom, and a wallpaper that is visible but never in the way. Nothing should need explaining, nothing should look half-built, and nothing should ask the user to finish building the home screen before using the phone. The rest — themes, gestures, folders, widgets, customisation — is depth the user discovers after the first impression has already landed.

---

## 7. Contrast study — Apple Human Interface Guidelines and iOS Home Screen (SpringBoard)

*Scope note.* Apple does not publish a single "SpringBoard design" page. The findings below are drawn from the Human Interface Guidelines (App icons; Icons; Widgets; Layout; Typography; Accessibility — via the HIG index and its mirrors), Apple Support's Home Screen and widget documentation, and published iOS design writing. Where a figure comes from a secondary source rather than Apple itself, that is stated. iOS version notes are given where they matter (iOS 14 widgets, iOS 16 Lock Screen widgets, iOS 18 free placement and Dark/Tinted icons, iOS 26 Liquid Glass and the Clear icon style). Marlow is an Android launcher; nothing in this section is a requirement to copy iOS. It is a contrast study: what Apple does differently, what is worth learning from, and what is not.

### 7.1 How iOS lays out the Home Screen

**The grid is fixed and small.** The iPhone Home Screen is a rigid grid — reported as 6 rows × 4 columns plus a 4-slot Dock on iPhone (28 places in total on a page). Icons cannot be dragged to arbitrary positions: every layout decision is a choice of which of those cells to fill. Until iOS 18, icons filled strictly top-left to bottom-right with no gaps; since iOS 18, icons can be placed in any open space on the grid, including just above the Dock, leaving gaps, columns, or rows empty — but they still snap to the grid. There is no user-chosen grid size (no 5×5 or 6×6 option), no icon-size slider for the grid itself, and no third-party launcher: SpringBoard is part of the system shell, not a replaceable app.

**The Dock is four slots, not five.** The iPhone Dock holds four apps, stays the same on every page, and is conventionally Phone, Messages, Safari, and Camera (or the user's four most-used). The four-slot Dock is the single most visible structural difference from Pixel's five-slot Dock, and it is a consequence of the four-column grid: the Dock is one grid row, visually separated, with the same column rhythm as the grid above it.

**Pages, dots, and the App Library.** Home Screen pages are swiped horizontally; dots near the bottom show how many pages there are, with the brighter dot marking the current page (in iOS 16 and later a Search button sits in that spot until the user starts swiping, when the dots appear). Individual pages can be hidden (jiggle mode → tap the dots → uncheck a page) without deleting their apps. The App Library is the last page to the right: every installed app, automatically sorted into categories (Productivity, Social, Utilities, and so on), with search. Newly downloaded apps can be sent to the App Library only, keeping the Home Screen clean — a setting under Settings → Home Screen. Removing an app from the Home Screen does not uninstall it; it stays filed in the App Library. That is the iOS answer to the problem Android answers with the app drawer, and it is a *categorised* answer rather than an alphabetical one: the system files apps for the user instead of presenting a flat list.

**Editing is a mode, not a gesture.** All rearrangement happens in "jiggle mode": touch and hold an empty spot (or an app → Edit Home Screen) until the icons wobble. In jiggle mode the user drags apps, drags one app onto another to make a folder, drags to the screen edge to move to another page, taps "−" to remove, and taps Done (or the background) to save. The jiggle itself is feedback: the whole screen is visibly in an editing state, so there is no ambiguity about whether a drag will launch or move. Android launchers, by contrast, usually enter edit mode per-item (long-press one icon); the iOS model makes the *mode* explicit and the *exit* explicit.

**Free placement is new, and deliberately limited.** iOS 18's free placement lets icons sit anywhere on the grid, including framing a wallpaper — but it does not let icons sit off-grid, overlap, or resize. The freedom is "which cells are empty", not "where exactly does this icon go". That restraint is the point: even Apple's most customisable Home Screen still guarantees alignment.

### 7.2 Widgets on iOS: fixed sizes, stacks, and Smart Stacks

**Three sizes, and only three.** iOS Home Screen widgets come in system sizes — small (2×2 icon spaces), medium (4×2), and large (4×4). The HIG gives the point sizes per device; for example, on a 393×852 pt screen (iPhone 14 Pro / 15 / 15 Pro / 16) small is 158×158 pt, medium 338×158 pt, and large 338×354 pt; on a 430×932 pt screen small is 170×170 pt, medium 364×170 pt, large 364×382 pt. There is no free resize, no 3×2 or 5×1 widget: a widget is one of the three footprints, and its content is designed for that footprint. The size is chosen when the widget is added (swipe through size options in the gallery), and changing size later means removing and re-adding, or using the long-press size menu.

**One widget, one job, one figure at small size.** The small widget is for one number — 72°, 9 days left, a battery percentage. Medium fits two lines of text, a label plus a bar, or today's event with a time. Large is for a calendar, a photo, or a countdown. The HIG's own Calendar example makes the progression explicit: small shows only the current date and one event; medium repeats the small information and adds a birthday and tomorrow's events; large adds time-of-day indicators on both sides. Each size *adds* information; none of them re-lays-out the same information at a different scale.

**Stacks and Smart Stacks are the iOS answer to widget clutter.** Any widgets of the same size can be stacked by dragging one onto another — up to 10 widgets in a stack — and the user swipes through them. A Smart Stack is a pre-built stack that rotates automatically, surfacing the most relevant widget based on time, location, and activity (with Smart Rotate and Widget Suggestions toggles, and reordering by dragging within the stack). This is the single most-copied-worthy iOS idea in this whole study: on Android, each widget occupies a permanent spot whether it is needed at the moment or not, and a screen with several different-sized, different-radius widgets "ends up looking haphazard, no matter how much you try". Stacking lets one slot serve several jobs, keeps the grid calm, and makes the Home Screen feel organised "just because" the widgets share one footprint and one corner radius. The Android-side objection — that Android's variable grid sizes and fragmented widget ecosystem make standardised stacks hard — is real, but it is an implementation difficulty, not a reason the idea is wrong.

**Widgets are glanceable and interactive, but constrained.** The HIG frames widgets as "timely, glanceable content" offering "specific functionality", appearing across Home Screen, Lock Screen, Today View, StandBy, and other contexts, adapting their appearance to each context and to the user's light/dark/clear/tinted customisation. They support interactive elements, but they are not mini-apps: hierarchy and graceful truncation matter because the widget is a constrained view. Lock Screen widgets (iOS 16+) are a separate, smaller family — circular, rectangular, and inline accessory widgets showing a very limited amount of information.

**Appearances: light, dark, clear, tinted.** On the iPhone Home Screen, widgets (like icons) can appear in light, dark, clear (translucent), and tinted appearances. The system provides default treatments per size and context; a custom widget design has to survive all four.

### 7.3 Folders on iOS

Folders are created the same way as on Android — drag one app onto another in jiggle mode, then drag more apps in; tap the name field (or long-press → Rename) to rename. The folder icon is a translucent rounded square showing a mini-grid of the contained apps' icons; tapping it opens a full-screen-ish sheet showing the folder's contents at full icon size with the name at the top. There is no iOS equivalent of Android's "large folder" or a folder that opens inline on the grid — the folder is always a container you enter and leave, and tapping empty space closes it.

Two iOS folder behaviours are worth noting for Marlow. First, the App Library *is* folders, done automatically: every app is filed into a category folder whether the user organises or not, so a user who never creates a folder still has an organised overflow. Second, iOS folders have no badge count of their own that aggregates their contents in a useful way on the closed folder — the closed folder shows the mini-icons, and badges live on the individual apps inside. A launcher that sums badge counts onto a closed folder is inventing a signal the platforms do not define.

### 7.4 App icons on iOS: the squircle, the 1024 master, and layered icons

**One master, one mask, no transparency.** Every modern Apple app icon is designed on a 1024×1024 px master canvas, exported as a flat PNG in sRGB (or Display P3 for wide gamut), fully opaque with no alpha channel, and with square corners. Xcode generates all smaller sizes from the master (iPhone Home Screen 180×180 px at 60 pt @3x; 120×120 px at @2x; iPad, Spotlight, Settings, and notification sizes below that). The system applies the mask; the designer must not pre-round corners — pre-rounded art sits inside the system mask and produces a doubly-rounded, too-tight corner, and can trigger App Store rejection.

**The mask is a squircle, not a rounded rectangle.** iOS clips all icons to a continuous curve (a squircle — a superellipse with continuous curvature, G2 continuity — rather than a sudden transition from straight edge to arc). The effective parameters are approximately 22.37% corner radius relative to side length with approximately 60% corner smoothing. In practice the shape is a continuous-curve rounded rectangle. The practical rule for Marlow is not the percentage — it is that Apple's corner *feels* softer and more continuous than a plain rounded rectangle at the same radius, because the curvature eases in. A launcher that wants a stock-Android squircle and an iOS-like squircle to feel related should use a continuous-curve corner, not a circular-arc corner.

**Safe zone by margin, not by a circle.** Apple's icon grid leaves roughly 100 px of margin on each side of the 1024 canvas for the safe zone; the primary symbol sits centred, with its visual centre slightly above the mathematical centre (optical centring). The squircle clips approximately 10% from each corner — important elements stay away from the edges. Text in icons is discouraged (words only if part of a logo); photos, screenshots, interface elements, and Apple hardware are banned from icons by the HIG.

**Layered icons and Liquid Glass (iOS 26).** Although a flattened image is accepted, layers give the most control: iOS, iPadOS, macOS, and watchOS icons have a background layer and one or more foreground layers that coalesce to create dimensionality, and on iOS 26 they take on Liquid Glass attributes — specular highlights, refraction, translucency — that respond to the environment and to interaction. Icons are authored in Icon Composer (included with Xcode), where the designer defines the background, places foreground layers, applies highlights and refraction, annotates the Default, Dark, and mono appearance variants, and previews across system versions. The HIG's layer rules: prefer clearly defined edges in foreground layers (avoid soft, feathered edges, so system-drawn highlights and shadows look their best); vary opacity in foreground layers to increase depth; design a background that stands out and emphasises the foreground, and if it is a gradient, make sure it responds well to system lighting effects.

**Dark, Tinted, and Clear variants.** iOS 18 introduced three user-selectable icon appearances — Light (Default), Dark, and Tinted — and iOS 26 added Clear (a translucent, glassy icon, in light/dark/auto). Since iOS 18, App Store Connect has required developers to provide the dark and tinted versions alongside the default. The dark version uses a dark gradient (reported as #313131 at the top to #141414 at the bottom) behind the mark; the tinted version is a single-channel luminance image the system re-colours from the wallpaper — not a filter over the default. The styles apply to apps and widgets on every page at once; there is no per-icon colour. A "Large" icons toggle removes all labels. These are the direct iOS analogues of Android's themed/monochrome icons — and the differences are instructive (see §7.7).

### 7.5 Labels and badges on iOS

**Labels.** iOS Home Screen labels are a single line under each icon, in the system font (SF Pro), at a small caption size, in the system label colour (white with a shadow, or dark, depending on wallpaper/appearance). Since iOS 18 the user can toggle labels off entirely, and the Large-icons option removes labels as part of making icons bigger. The label is the app's name, truncated with an ellipsis when it does not fit — the same one-line rule as Android, for the same reason: a second line would break the grid's vertical rhythm.

**Badges are always red, always a number, always per-app.** An iOS badge is a small red circle with a white number at the top-right corner of the app icon, counting unread or pending items as the app defines them (unread conversations in Messages, missed calls and voicemails in Phone, unread mail in Mail, a pending update in Settings/App Store). Badges are controlled per app in Settings → Notifications → [app] → Badges; there is no single switch that hides every badge. Badges are *off* in the App Library by default — they only appear there if the user turns on Settings → Home Screen & App Library → Show in App Library. Focus modes can hide badges while active. The badge colour cannot be changed.

The contrast with Android matters: Android's native signal is a *dot* (presence, not quantity) tied to active notifications, with the count visible only in the long-press menu; iOS's signal is a *number* (quantity) tied to the app's own unread count, independent of whether a notification is currently posted. Neither is "correct" — but a launcher must pick one model and be consistent. A launcher that shows a dot for some apps and a number for others, or a number that disagrees with the app's own count, is worse than either model done cleanly.

### 7.6 Typography, colour, and layout foundations (HIG)

Apple's HIG rests on three pillars — **clarity** (every element legible and purposeful), **deference** (the interface never overshadows the content), and **depth** (layering, transitions, and realistic motion convey hierarchy). The foundations that translate most directly to launcher work:

- **Layout and safe areas.** Respect safe areas, layout margins, and guides; safe areas avoid the Dynamic Island, camera housing, and bars. Never hand-place content where the system reserves space. Standard content margins are 16–20 pt from screen edges; spacing increments are 8 / 16 / 24 pt. Design for the smallest screen first (375 pt width). Backgrounds and decoration may run edge-to-edge under floating controls; interactive content must not.
- **Typography.** Use the system fonts (SF Pro, SF Compact, New York) unless brand demands otherwise — they ship optical sizing, correct tracking, and Dynamic Type for free. iOS body default is 17 pt; the minimum is 11 pt (captions/secondary only). Convey hierarchy with the built-in text styles (Large Title, Title 1–3, Headline, Body, Callout, Subheadline, Footnote, Caption 1–2), which scale correctly with Dynamic Type. Support Dynamic Type: test at the largest accessibility sizes; layouts must reflow, not clip or truncate. Limit the number of typefaces and styles on a screen.
- **Colour.** Prefer system semantic colours; never hard-code system colour values (documented values shift between releases). Ship light + dark + increased-contrast variants for every custom colour. Never rely on colour alone to convey state or meaning — pair it with text, shape, or a symbol. Contrast: 4.5:1 minimum for text/icons (WCAG AA), striving for 7:1 on custom small text; verify in both appearances and with Increase Contrast / Reduce Transparency on.
- **Icons (interface icons / SF Symbols).** SF Symbols is a library of 6000+ symbols that match the system fonts, ship in multiple weights and scales, and adapt to Dynamic Type and Dark Mode. Match symbol weight and scale to the adjacent text; tint with dynamic colours or vibrancy. Maintain visual consistency across all interface icons in an app — consistent size, level of detail, stroke thickness (weight), and perspective — and optically align asymmetric icons rather than centring them geometrically. Include text in an icon only when essential.
- **Materials.** A material is a visual effect that creates depth, layering, and hierarchy between foreground and background elements. In the current (Liquid Glass) system, the screen splits into a content layer (standard materials — blur, vibrancy — never Liquid Glass) and a functional layer (controls and navigation floating above content on Liquid Glass, refracting whatever scrolls beneath). Content extends to the screen edges and under the floating controls; controls live on a separate plane, not inline.
- **Touch targets.** Minimum 44×44 pt for interactive elements (28×28 pt absolute visual minimum, with the hit area still aiming for 44). Spacing around bezeled controls is about 12 pt; around borderless controls, about 24 pt from visible edges.

### 7.7 Android vs. iOS — the differences that matter

| Aspect | Android / Pixel Launcher | iOS / SpringBoard | What it means for Marlow |
|---|---|---|---|
| Launcher replacement | Any app can be the default launcher | Not allowed; SpringBoard is the system shell | Marlow *is* the variable part on Android — it has to earn the "stock" feeling; iOS gets it for free by being the only option |
| All installed apps | App drawer, swipe up, alphabetical | App Library, last page right, auto-categorised | A categorised overflow (like App Library) hides clutter better than an alphabetical list; Marlow's drawer could offer a category view without copying Apple |
| Grid | User-chosen (5×5 / 4×5 on Pixel; other launchers offer more) | Fixed 4 columns, 6 rows + 4-slot Dock; free placement within the grid since iOS 18 | Choice is Android's strength and its risk: Marlow must make the *default* grid feel as inevitable as iOS's fixed grid |
| Dock | 5 slots (Pixel) | 4 slots | Do not copy the 4-slot Dock onto a 5-column grid — the Dock should rhyme with the grid's column count |
| Icon shape | OEM mask: circle (Pixel), squircle (Samsung), etc.; launcher applies one mask | One squircle everywhere, ~22.37% radius, continuous curve | One mask, applied consistently, is the whole lesson; the exact radius is a flavour choice |
| Icon layers | Foreground + background (108 dp), optional monochrome | Background + foreground layers in Icon Composer; 1024 px master, opaque, square corners | Both platforms converged on "layers, system applies the mask, designer keeps the mark in the safe zone" — that convergence is the rule, not either platform's numbers |
| Icon theming | Themed icons: monochrome layer tinted from wallpaper (API 33+); auto-theming on Android 16 QPR 2 | Dark / Tinted / Clear appearances; tinted is a luminance image re-coloured by the system; styles apply to all icons at once | iOS theming is *system-wide and uniform* (every icon gets the same treatment); Android theming is *per-app and mixed* (only apps with a monochrome layer theme). Marlow must design for the mixed grid, not pretend it is uniform |
| Widgets | Resizable to whole cells, many sizes, only touch + vertical swipe | Fixed small/medium/large footprints; interactive since iOS 17; stackable to 10, Smart Stack rotates by context | Fixed sizes are what make iOS widget screens look calm; free resize is what makes Android widget screens look haphazard. Marlow should offer a small set of canonical widget sizes and make stacking easy |
| Widget overflow | More widgets = more pages | Stacks + App Library absorb overflow | Stacking is the idea to steal (see §7.8) |
| Folders | Circular-window preview (4 mini-icons), opens as grid/sheet | Translucent square, mini-grid preview, opens as sheet | Same gesture, same mental model; the preview must show contents, not a generic glyph |
| Labels | One line, ellipsis, 12sp band, on by default on the grid | One line, ellipsis, SF caption size; can be toggled off; Large icons remove labels | Same rule, same exception: labels off is an explicit user choice, never the default on the grid |
| Badges | Dot by default (presence), count in long-press menu, tied to active notifications | Red number always (quantity), tied to the app's unread count, per-app switch, off in App Library by default | Pick one model per surface and be consistent; do not mix dots and numbers on the same screen |
| First boot | Launcher chooses the default layout | Apple chooses the default layout | Marlow's first boot *is* its App Library moment — the default must feel chosen, not empty |
| Customisation philosophy | Deep customisation is the platform norm | Customisation arrived gradually (widgets iOS 14, free placement + tinting iOS 18, Clear iOS 26) and is still bounded | iOS proves that a bounded, well-chosen set of options can feel more premium than unlimited options. Marlow's plan already encodes this as the toggle rule (extra/Pro features off by default) |

### 7.8 Worth learning from iOS

1. **Stacks / Smart Stacks.** One slot, several widgets, swipe or auto-rotate by time/location/activity, up to 10 per stack. This is the best idea in the iOS Home Screen for an Android launcher: it solves widget clutter without deleting widgets, and it keeps the grid calm. Marlow should treat widget stacking as a first-class feature, not a later extra — with the same-size-only rule (a stack holds widgets of one footprint) and a visible affordance (dots) so the user knows there is more in the slot.
2. **The fixed grid as a design constraint, not a limitation.** iOS looks calm because the grid cannot be broken. Marlow cannot (and should not) remove grid choice on Android, but its *default* grid should be as disciplined as iOS's fixed grid: one size, one gap, one label rule, no half-cell placements.
3. **One mask everywhere, applied by the system.** The squircle is not magic; the magic is that there is exactly one shape and nobody gets an exception. Marlow's mask discipline (§3.1) is the Android version of the same idea.
4. **Labels off is a mode, not a default.** iOS lets the user remove labels — but only as an explicit, whole-screen choice (Large icons / label toggle), never per-icon and never by accident. Marlow should copy that shape: a single, explicit, reversible setting.
5. **Badges as a scarce, per-app, user-controlled signal.** iOS badges are per-app, always the same colour, always the same shape, and off in the overflow (App Library) by default. Marlow should copy the discipline (one colour, one shape, per-app control, no badges in the drawer by default) even though Android's native signal is a dot rather than a number.
6. **The App Library as an organised overflow.** Auto-categorised, searchable, and the default destination for new apps. Marlow's drawer does not need to copy Apple's categories, but a category view and a "new apps go to the drawer, not the home screen" default would reproduce the effect that keeps iOS home screens clean.
7. **Semantic colour and type, not hard-coded values.** iOS's semantic colours and text styles adapt to Dark Mode, Increase Contrast, and Dynamic Type for free. Marlow's Material You / `MaterialTheme.typography` approach is the Android equivalent — the lesson is to never hard-code a label colour or size that a system setting should own.
8. **Deference: the interface never overshadows the content.** The wallpaper and the user's apps are the content; the launcher's chrome (dock, search pill, folder sheets) should defer to them. A launcher that draws attention to its own chrome has misunderstood its job.

### 7.9 What not to copy from iOS

1. **The 4-column grid and 4-slot Dock.** They belong to a 4-column phone. On a 5-column Android grid, a 4-slot Dock would leave a hole at one end and break the column rhythm. Copy the *principle* (Dock rhymes with grid), not the number.
2. **Liquid Glass.** The plan for Marlow already excludes Liquid Glass (custom blur glass / Opaque only). iOS 26's specular highlights, refraction, and translucency are a system-wide material with OS-level support; reproducing them as a launcher-level effect on Android would be a performance and legibility risk, and the HIG itself warns that content layers should use standard materials (blur, vibrancy), never Liquid Glass. Marlow's "soft subtle halo" and real blur radius are the right scale of ambition.
3. **No third-party launcher, no icon packs, no grid choice.** These are platform restrictions, not design choices. Android users choose a launcher *because* it offers choice; removing choice to imitate iOS would remove Marlow's reason to exist.
4. **The jiggle.** Jiggle mode is a clear editing state, and its explicitness is worth learning from — but the wobble animation itself is an iOS mannerism. Marlow should have an explicit edit mode with a clear exit; it does not need to copy the wobble.
5. **Forcing every icon into one appearance.** iOS can apply Dark/Tinted/Clear to every icon at once because Apple controls the icon pipeline and requires the variants. On Android, only apps with a monochrome layer can theme, and the mixed grid is documented behaviour. A launcher that force-tints every icon with a colour filter to imitate iOS's uniform tinted look will produce muddy icons (§3.3, mistake #10).
6. **The App Library's auto-categories as the only organisation.** Auto-categories are good for overflow and bad as a primary organisation: they guess, they are wrong sometimes, and they cannot know the user's own groupings ("my travel apps" is not an App Store category). Marlow should offer categories as a *view*, not as the *only* view.
7. **Hiding the drawer.** iOS has no drawer button because it has no drawer — the App Library is a page. Android users expect a drawer, and Pixel's swipe-up drawer is a learned gesture. Marlow should keep the drawer and make it good, not hide it to look like iOS.

---

## 8. CHECKABLE RULES

Each rule is written so it can be tested against the running app. "Pass" means the stated condition holds on a Pixel-sized phone (1080×2400, ~420 dpi) at default settings, unless the rule says otherwise. The source in brackets is the justification; where a rule synthesises several sources, the most direct one is cited and the synthesis is noted in the text above.

### A. Home-screen layout

- **R1 — First boot is not empty.** On a fresh install with a normal set of installed apps, page one shows: an At a Glance / clock-weather strip, at least 8 app icons or folders, and a 5-icon dock. No empty page, no "add apps to get started" placeholder as the home screen. [Pixel Launcher first-boot convention; pcnmobile.com home-screen layout guidance; §2.1]
- **R2 — One glanceable strip, at the top, full width.** The top strip shows date + weather and occupies the full width in a single row. It is the only live/updating element in the top zone. [support.google.com/android/answer/14883154?hl=en-CA]
- **R3 — No duplicated facts on page one.** The time appears in at most two places (status bar + one clock/glance element) and the weather in at most one place. No screen shows weather in At a Glance *and* in a separate weather widget by default. [§2.3; pcnmobile.com]
- **R4 — Page one holds 8–12 app/folder cells plus the dock.** Count cells on page one at default settings: between 8 and 12 app or folder cells in the grid, plus exactly 5 dock icons. Fewer than 8 reads as empty; more than 12 (excluding widgets) reads as a shelf. [§2.1 synthesis; pcnmobile.com — 4–8 most-used apps + dock]
- **R5 — Grid is 5 columns by default (4×5 on smaller phones).** The default grid matches the Pixel convention (5×5, or 4×5 on smaller phones). Any other grid is a user setting, not the default. [geekchamp.com/make-nova-launcher-look-and-work-like-pixel-launcher/]
- **R6 — Dock has exactly 5 icons, one row, one page.** No second dock page, no dock widget, no two-row dock. Dock icon size equals grid icon size (100–110% scale). [geekchamp.com/make-nova-launcher-look-and-work-like-pixel-launcher/]
- **R7 — Dock contents are the daily five by kind.** By default the dock holds, by kind: Phone, Messages (or the user's messenger), browser, mail, Camera. No game, social, or store app in the default dock. [geekchamp.com/make-nova-launcher-look-and-work-like-pixel-launcher/; androidauthority.com/why-i-stick-with-default-pixel-launcher-3616782/]
- **R8 — Dock is stable across pages.** Swiping to any home-screen page shows the same five dock icons in the same order. No suggested or recent apps in the dock. [howtogeek.com/345451/how-to-get-started-with-androids-home-screens/; §2.2]
- **R9 — No drawer button in the dock.** The app drawer opens by swipe-up only. No icon in the dock is spent on a drawer button. [blog.rottenwifi.com/pixel-launcher-everything-you-need-to-know-about-googles-pixel-home-screen/ — Pixel Launcher removed the drawer button]
- **R10 — One search entry point, thumb-reachable.** Exactly one search entry point on the home screen (search pill/bar), at the bottom, reachable without scrolling. No second search bar at the top. [blog.rottenwifi.com/pixel-launcher-everything-you-need-to-know-about-googles-pixel-home-screen/]
- **R11 — At most one widget on first boot, and it is not a duplicate.** First boot shows at most one widget besides the glanceable strip, and it does not repeat information already in the strip. [pcnmobile.com — "one or two genuinely useful widgets"; §2.5]
- **R12 — Widgets align to the grid.** Every widget's edges align to grid column/row lines. No widget is offset by half a cell. Resize snaps to whole cells. [developer.android.com/guide/topics/appwidgets/overview?authuser=120; http://developer.android.com/guide/practices/ui_guidelines/widget_design.html?authuser=517]
- **R13 — Widget size follows its job.** A 2×2 widget shows one number or fact; a 4×2 widget shows at most a label plus a bar or two lines; a 4×4 widget is used only for a calendar, photo, or countdown-type content. A large widget showing one line of text fails. [developer.android.com/guide/topics/appwidgets/overview?authuser=120; §2.5]
- **R14 — Widgets are teasers, not apps.** Tapping a widget's information element opens the relevant detail screen in the app; the widget itself does not contain a full list, feed, or scrolling content that duplicates the app. [developer.android.com/guide/topics/appwidgets/overview?authuser=120]
- **R15 — Widgets support only touch and vertical swipe.** No horizontal swipe, no in-widget drag, no in-widget long-press gesture that conflicts with home-screen gestures. [developer.android.com/guide/topics/appwidgets/overview?authuser=120]
- **R16 — Folders are created by drag-on-top.** Dragging one app icon onto another creates a folder. No Settings-only folder creation as the primary path. [blog.rottenwifi.com/pixel-launcher-everything-you-need-to-know-about-googles-pixel-home-screen/]
- **R17 — Closed folder shows its contents.** A closed folder's preview shows clipped mini-icons of the contained apps (up to four visible), in a stable order, inside a circular-window-style preview — not a generic folder glyph. [androidpolice.com/2016/04/13/android-n-dev-preview-2-includes-a-new-style-for-google-now-launcher-folders/ — URL as returned: http://www.androidpolice.com/2016/04/13/android-n-dev-preview-2-includes-a-new-style-for-google-now-launcher-folders/]
- **R18 — Open folder is a grid with an editable name.** Tapping a folder opens a sheet/grid of full-size icons with the folder name at the top; tapping the name allows renaming; tapping empty space outside closes it; the home grid behind does not shift. [blog.rottenwifi.com/pixel-launcher-everything-you-need-to-know-about-googles-pixel-home-screen/]
- **R19 — A folder is one grid cell.** A folder occupies exactly one cell, with the same size and label position as an app icon. No oversized or differently-shaped folder on the grid. [§2.6]
- **R20 — At most two folders on first boot, and both are named.** First boot ships at most two folders, each with a short task-based name (e.g. "Google"). No folder named "Folder" or left unnamed. [§2.6; androidauthority.com/why-i-stick-with-default-pixel-launcher-3616782/]
- **R21 — Visible empty space on page one.** At default settings, at least one full grid row (or its equivalent in widget-free space) on page one is empty. A completely full first page fails. [§1.3; geekchamp.com/make-nova-launcher-look-and-work-like-pixel-launcher/ — "a mostly empty first page"]
- **R22 — No tips card, settings shortcut, or store shortcut on page one.** Page one contains no onboarding card, no Settings shortcut, no Play Store shortcut, and no promotional element at default settings. [§1.6; §5 mistake #4]
- **R23 — Scroll/motion is Simple or None.** Page transitions use the Simple or None scroll effect. No cube, card-stack, zoom, or bounce transition at default settings. [geekchamp.com/make-nova-launcher-look-and-work-like-pixel-launcher/]

### B. Iconography — adaptive icons and consistency

- **R24 — Marlow's own icon is a proper adaptive icon.** It ships foreground + background layers at 108×108 dp (vectors preferred), with the mark at 48–66 dp inside the centred 66×66 dp safe zone, and a monochrome layer for themed icons. [developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4]
- **R25 — Marlow's own icon survives every mask.** Render the icon under circle, squircle, rounded-square, and teardrop masks: the mark is never clipped and remains recognisable in all four. Test at 48 px, not only at 512 px. [developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4]
- **R26 — No baked-in mask or shadow in any icon layer.** No icon layer shipped by Marlow contains a pre-drawn mask, rounded corners, or a drop shadow around its outline. The launcher supplies mask and shadow. [developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4]
- **R27 — One mask for every app icon.** Every adaptive icon on the home screen and in the drawer is rendered with the same mask shape. No app shows a circle while its neighbour shows a squircle. [developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4 — "Each device OEM must provide a mask, which the system uses to render all adaptive icons with the same shape"]
- **R28 — Legacy icons are normalised onto a tray.** Apps without an adaptive icon are rendered on a consistent tray at a fixed scale (one scale for all legacy icons), not at their raw packaged size and shape. No raw legacy square sits next to a masked adaptive icon. [§3.1; dev.svgmaker.io/docs/tutorials/consistent-svg-app-icons-for-ios-android]
- **R29 — One apparent icon size across the grid.** Measure the rendered mark size (not the cell size) for ten varied apps: the marks fall within a narrow band (target roughly 54–60 dp of visual size within the 108 dp canvas). No icon looks obviously larger or smaller than its neighbours. [§3.2; developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4 — 48–66 dp logo range]
- **R30 — One shadow (or none), everywhere.** All icons share the same drop-shadow treatment (same blur, same offset, same opacity — or no shadow at all). No icon has a heavier or differently-coloured shadow than the rest. [§3.2; m3.material.io/styles/icons/designing-icons/ — consistent visual style in one set]
- **R31 — Launcher-drawn chrome is neutral.** Folder backgrounds, the search pill, and the dock background use neutral surfaces or the Material You palette — not saturated brand colours, not per-folder accent colours. The grid's colour comes from the app icons, not from the launcher. [§3.2; m3.material.io/styles/icons/designing-icons/]
- **R32 — System icons follow M3, in one style.** All of Marlow's own interface icons (Settings, drawer, sheets) are drawn on the 24dp grid, 20dp live area, 2dp stroke (weight 400), 2dp corner radius, in a single style — outlined *or* filled *or* rounded, never mixed in one screen. [m3.material.io/styles/icons/designing-icons/]
- **R33 — Marlow's monochrome layer is a true silhouette.** The themed-icon layer is a single-colour solid silhouette of the primary mark only — no gradient, no two-tone, no drop shadow, no thin detail below the legibility threshold. It reads as the same mark when tinted. [developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4; github.com/ichintansoni/skills-master/blob/HEAD/skills/android/design/technologies/m3-app-icons/SKILL.md]
- **R34 — Themed icons are tested against a range of tints.** Marlow's monochrome icon is checked against at least four wallpaper-derived tints (warm, cool, desaturated, neutral) on both light and dark backgrounds, and remains recognisable in all of them. [github.com/ichintansoni/skills-master/blob/HEAD/skills/android/design/technologies/m3-app-icons/SKILL.md — "Test the monochrome layer against a range of tint colors and backgrounds"]
- **R35 — No forced tinting of apps without a monochrome layer.** Below Android 16 QPR 2, apps without a monochrome layer keep their normal icons when themed icons are on. Marlow does not apply its own colour filter to them. On Android 16 QPR 2+, the system's auto-theming is used, not a launcher-invented tint. [developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4 — themed-icon fallback cases; itechguides.com/android-13s-themed-icons-what-they-do-and-how-to-enable-them/]
- **R36 — Icon has no text (or a single bold letterform only).** Marlow's own icon contains no word, phrase, or sentence. A single bold letterform is the only text exception. [tms-outsource.com/blog/posts/designing-an-icon-for-the-google-play-store/ — "Text inside the icon almost always hurts"]

### C. Icon labels

- **R37 — Labels are 12sp, one weight, one colour treatment.** All home-screen app labels use the same size (12sp, `labelMedium` scale), the same weight, and the same colour treatment. No label is smaller than 11sp; no body text anywhere on the home screen is below 12sp. All sizes use `sp`. [m3.material.io type scale via github.com/hamzaviour/whizzlylab/blob/HEAD/.agent/skills/android-design-guidelines/SKILL.md — labelMedium 12sp/500, labelSmall 11sp/500, minimum 12sp body / 11sp labels]
- **R38 — Labels are one line, truncated with an ellipsis.** No home-screen label wraps to two lines. A label that does not fit is truncated with "…" — never clipped mid-glyph, never shrunk to fit. [§3.4; androidcentral.com/switching-android-android-theming-puts-apples-aesthetics-shame — the "Google P…" symptom]
- **R39 — Labels survive 200% font scaling.** At 200% font scale, no label overlaps the icon below it, no label overlaps a neighbouring label, and every label still truncates (rather than clipping) within its cell. [github.com/hamzaviour/whizzlylab/blob/HEAD/.agent/skills/android-design-guidelines/SKILL.md — "Test at 200% font scale. Ensure no text is clipped or overlapping"]
- **R40 — Labels are on by default on the grid.** App labels are visible on the main grid at default settings. Hiding labels is an explicit, whole-screen, opt-in user setting; it does not change cell height (the label slot becomes breathing room). [geekchamp.com/make-nova-launcher-look-and-work-like-pixel-launcher/ — "Keep Label enabled if you want a stock Pixel-style home screen"]
- **R41 — Every cell has the same height.** Icon + fixed label slot is identical for every cell, whether the label is short, long, truncated, or hidden by the user setting. Row baselines are even across the whole grid. [m3.material.io/foundations/layout/grids-spacing/grids; §4.3]

### D. Badges

- **R42 — Badge is a dot, pinned top-right, not moving the icon.** The notification dot sits at the top-right corner of the icon, overlapping the edge, in the same position for every icon, and does not shift the icon, the label, or neighbouring cells. [developer.android.com/develop/ui/views/notifications/badges — dots appear on the launcher icon from API 26; threads.com/@designmotionhq/post/De_4ze5jPZo — "pin the badge to the top-right corner so the icon doesn't shift"]
- **R43 — One badge colour, one badge shape.** All badges use one colour and one shape (dot). No per-app badge colours, no mixed dots and numbers on the same screen. [threads.com/@designmotionhq/post/De_4ze5jPZo; instagram.com/reel/Dcnuc60NbeO/ — "show only one signal per row (dot or number, never both)"]
- **R44 — Numeric badges, if offered, cap at 99+.** Any numeric badge counts up to "99+" and no higher, so the badge width stays stable as the count grows. [threads.com/@designmotionhq/post/De_4ze5jPZo; instagram.com/reel/Dcnuc60NbeO/ — "cap counts at 99+"]
- **R45 — Badge clears when the app is opened.** Opening the app (or its relevant view) clears its badge immediately. A badge that survives opening trains the user to ignore badges. [threads.com/@designmotionhq/post/De_4ze5jPZo — "clear the badge immediately upon opening the associated view"]
- **R46 — Badges are per-app controllable and respect the system.** The user can turn badges off per app, and Marlow respects the system's per-channel `setShowBadge(false)` / notification state rather than maintaining a disagreeing count of its own. [developer.android.com/develop/ui/views/notifications/badges]
- **R47 — Badges only for decisions.** Badges appear only for items that require a user decision/action (unread messages, missed calls). No badge for "something happened" with no action needed, and no badge on every icon. [threads.com/@designmotionhq/post/De_4ze5jPZo — "badge only decisions, not everything"]

### E. Legibility, scrims, and spacing rhythm

- **R48 — Labels meet 4.5:1 against the worst patch behind them.** Measured against the lightest wallpaper pixel in each label's band (for light labels), every home-screen label achieves at least 4.5:1 contrast on each of four test wallpapers: bright photo, dark photo, busy photo, flat colour. [shotcanvas.app/blog/text-over-images; onlinetools.world/blog/contrast-over-photos-and-gradients/; m3.material.io contrast floor via github.com/ephk15/design-guidelines/blob/HEAD/skills/m3-material/m3-foundations/SKILL.md — body text ≥ 4.5:1]
- **R49 — Scrim strength follows the wallpaper.** The label-zone scrim is strong over bright art and fades toward nothing over dark art (luminance-aware, e.g. via `WallpaperColors`), with a faint veil as the fallback when no wallpaper pixels can be read. A fixed-strength scrim on all wallpapers fails. [shotcanvas.app/blog/text-over-images — flat 30–50% black, gradient 50–70% where text sits; §4.2]
- **R50 — No visible scrim edge.** On a smooth light wallpaper, no line, band, seam, or shoulder is visible where any scrim starts or ends. Gradient scrims use eased intermediate stops. [shotcanvas.app/blog/text-over-images — "add intermediate stops so the darkening starts gently and accelerates"]
- **R51 — One gap token, everywhere.** Horizontal and vertical gaps between grid cells use a single spacing token from the 8dp scale (`space100 = 8dp` base). The icon-to-label gap is a single smaller token, identical for every cell. [m3.material.io/styles/spacing]
- **R52 — Edge padding exceeds the inter-cell gap.** Horizontal and vertical edge padding (medium/large) is visibly larger than the gap between cells. No icon touches the screen edge. [m3.material.io/styles/spacing; geekchamp.com/make-nova-launcher-look-and-work-like-pixel-launcher/ — medium/large horizontal padding]
- **R53 — Dock separation exceeds the row gap.** The space between the last grid row and the dock is visibly larger than the gap between two grid rows, so the dock parses as a separate zone. [§4.3]
- **R54 — Spacing is defined on the parent, not per child.** Padding and gaps are set on the grid/dock containers, not as individual margins on icon cells. No per-icon margin adjustments. [m3.material.io/styles/spacing — "Define padding and gaps on the parent container"; "Avoid defining margins on child elements"]
- **R55 — No user-visible internal names.** No Settings string, label, or dialog shows a feature number, phase name, plan reference, or developer aside. [§5 mistake #13]

### F. Apple contrast study — checkable rules for Marlow

These rules are Marlow rules *informed by* the iOS study in §7, not iOS requirements. Each is testable against the Android app.

- **A1 — Widget stacks exist and hold same-size widgets only.** Marlow supports stacking widgets of the same footprint (up to 10 per stack), with a visible affordance (dots) and swipe to move through the stack. Stacking widgets of different footprints is not allowed. [support.apple.com/en-us/118610 — "Drag a widget on top of another widget. You can stack up to 10 widgets"; developer.apple.com/design/human-interface-guidelines/widgets]
- **A2 — A Smart Stack equivalent rotates by context.** At least one stack type rotates automatically by time/location/activity (or by a user-chosen schedule), with Smart Rotate / suggestions toggles and drag-to-reorder inside the stack. [support.apple.com/en-us/118610 — Smart Stack "displays the right widget based on factors like your location, an activity, or time"]
- **A3 — Widget sizes are canonical, not free-form.** Marlow offers a small fixed set of widget footprints (small 2×2, medium 4×2, large 4×4, in grid cells) rather than arbitrary pixel resize. Each footprint's content is designed for that footprint — a larger size *adds* information, it does not stretch the smaller layout. [developer.apple.com/design/human-interface-guidelines/widgets; prettyprogress.app/blog/home-screen-widgets-iphone — small 2×2 / medium 4×2 / large 4×4]
- **A4 — The default grid is as disciplined as a fixed grid.** At default settings, no icon or widget can sit half a cell off the grid, overlap another item, or use a non-grid size. Grid choice exists as a setting, but the default is a single disciplined layout. [§7.1; webiano.digital/knowledge-base/mobile/mobile-fundamentals/home-screen/ — iOS grid is fixed, Android grid is a choice]
- **A5 — The Dock rhymes with the grid.** The Dock's slot count matches the grid's column logic (5 slots on a 5-column grid). Marlow does not ship a 4-slot Dock on a 5-column grid, or vice versa. [§7.7 table; iscreenapp.com/blog/iphone-home-screen-layout-ideas — iPhone 6×4 grid + 4-slot Dock]
- **A6 — Edit mode is explicit, with an explicit exit.** Entering home-screen edit mode is a visible state change (not just "this one icon is now draggable"), and there is an explicit Done/exit action. The user is never unsure whether a drag will launch or move. [solveyourphone.com/how-to-customize-iphone-home-screen-step-by-step-guide/ — jiggle mode and Done]
- **A7 — New apps do not land on page one by default.** Newly installed apps go to the drawer (or a user-chosen destination), not onto the first home-screen page, unless the user explicitly adds them. A setting offers the choice, with the drawer as the default. [techsith.com/how-to-change-iphone-home-screen-layout/ — "Choose New App Placement … Keeping them in App Library helps protect a clean first page"]
- **A8 — The drawer offers a categorised view.** As well as the alphabetical All Apps list, the drawer offers a category-grouped view (auto-categorised, searchable), so the overflow is organised rather than flat. [techetips.com/how-do-i-organize-my-apps-on-my-iphone-11/ — App Library "automatically files every installed app into categories"]
- **A9 — Labels-off is one explicit whole-screen setting.** Hiding labels is a single, explicit, reversible, whole-screen setting — never per-icon, never the default, and never changing cell height. [techetips.com/how-do-i-organize-apps-with-the-new-iphone-os/ — iOS 18 "toggle Large icons, which removes all labels, and toggle labels off separately"]
- **A10 — No badges in the drawer by default.** Notification badges do not appear on apps in the drawer/App Library view by default; they appear on home-screen icons (where the user has enabled them). A setting can enable drawer badges. [digitaltrends.com/tablets/how-to-control-notification-badges-on-iphone-and-ipad/ — "By default, notification counters do not appear in the App Library"]
- **A11 — iOS badge discipline is matched on the Android dot.** Marlow's dot follows the iOS badge discipline even though the shape differs: one colour (red family), one shape, per-app on/off, no badge colour customisation, no badge on apps with nothing actionable. [solveyourtech.com/what-is-a-badge-app-icon-on-the-iphone-6/ — "Badges on iPhone are the small red circles with a number … Settings > Notifications … Badges switch"; digitaltrends.com/tablets/how-to-control-notification-badges-on-iphone-and-ipad/]
- **A12 — Icon corners use a continuous curve.** Marlow's icon mask (and its folder/search-pill corner radii) use a continuous-curve (squircle-style) corner rather than a circular-arc rounded rectangle, so the curvature eases in rather than switching abruptly from straight edge to arc. [github.com/app-builders-club/design-builder/blob/HEAD/skills/design/references/ios/icons.md — squircle ~22.37% radius, ~60% smoothing, continuous curvature]
- **A13 — Label typography follows the platform's semantic scale.** Marlow's labels use `MaterialTheme.typography` roles (not hard-coded sizes) so they follow the user's font scaling, and they are verified at the largest accessibility sizes without clipping or overlap — the Android equivalent of iOS's Dynamic Type + 11 pt minimum discipline. [github.com/dodoxtech/ios-app-design-skills/blob/HEAD/skills/apple-hig-foundations/SKILL.md — body 17 pt, minimum 11 pt, Dynamic Type to AX5; §3.4]
- **A14 — Chrome defers to content.** The launcher's own chrome (dock, search pill, folder sheets, edit affordances) is visually quieter than the wallpaper and the app icons: neutral surfaces, no saturated chrome, no chrome that competes with the content for attention. [github.com/chrispiz/apple-app-ui-design/blob/HEAD/references/apple-hig.md — HIG pillars: clarity, deference, depth; §7.6]

---

## 9. The 10 rules most likely to matter for a stock-feeling launcher

If only ten rules are enforced before anything else, enforce these — they are the ones a user notices in the first five seconds, and the ones whose absence reads as "not stock" rather than "missing a feature".

1. **R1 — First boot is not empty.** A curated page 0 + dock + glanceable strip on fresh install. An empty home is the single biggest stock-feel failure.
2. **R27 — One mask for every app icon.** Every icon, same shape. Mixed masks are the fastest visual tell of an amateur launcher.
3. **R48 — Labels meet 4.5:1 against the worst patch behind them.** If the labels are unreadable on the user's photo, nothing else matters — the home screen has failed at its most basic job.
4. **R6 + R7 — Five-icon dock, the daily five by kind.** The dock is the anchor of the whole screen; five stable, familiar apps, never changing.
5. **R38 — Labels are one line, truncated with an ellipsis.** Two-line or clipped labels break the grid rhythm on every row they touch.
6. **R24 + R25 — Marlow's own icon is a proper adaptive icon that survives every mask at 48 px.** The launcher's own icon is the first icon the user sees; if it clips under a circular mask, the launcher looks broken before it is even opened.
7. **R33 + R35 — A true monochrome silhouette, and no forced tinting.** Themed icons are where Material You either looks unified or looks muddy; the difference is a real silhouette layer versus a colour filter.
8. **R42 + R43 — One dot, top-right, one colour, never moving the icon.** Badges are a scarce signal; a consistent, quiet dot preserves it, and a shifting or multicoloured badge destroys it.
9. **R21 — Visible empty space on page one.** Calm is a feature. A full first page reads as a shelf, not a home.
10. **A1 — Widget stacks (same-size only, up to 10, with dots).** The iOS idea most worth stealing: it is the only widget feature that *reduces* clutter while *adding* capability, and it is the clearest cure for the "several different-sized widgets looking haphazard" problem on Android.

---

## 10. Sources

Primary platform sources (read directly):

- Adaptive icons — Android Developers: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4
- Modify a notification badge — Android Developers: https://developer.android.com/develop/ui/views/notifications/badges
- App widgets overview — Android Developers: https://developer.android.com/guide/topics/appwidgets/overview?authuser=120
- App Widget Design Guidelines — Android Developers: http://developer.android.com/guide/practices/ui_guidelines/widget_design.html?authuser=517
- Icons — Material Design 3: https://m3.material.io/styles/icons/designing-icons/
- Icons overview — Material Design 3: https://m3.material.io/styles/icons
- Spacing — Material Design 3: https://m3.material.io/styles/spacing
- Grids & spacing — Material Design 3: https://m3.material.io/foundations/layout/grids-spacing/grids
- Use At a Glance on your Android device — Google Support: https://support.google.com/android/answer/14883154?hl=en-CA
- App icons — Apple Human Interface Guidelines: https://developer.apple.com/design/human-interface-guidelines/app-icons
- Icons — Apple Human Interface Guidelines: https://developer.apple.com/design/human-interface-guidelines/icons
- Widgets — Apple Human Interface Guidelines: https://developer.apple.com/design/human-interface-guidelines/widgets
- Human Interface Guidelines (index): https://developer.apple.com/design/human-interface-guidelines.md
- How to add and edit widgets on your iPhone — Apple Support: https://support.apple.com/en-us/118610
- Add widgets on iPhone — Apple Support: https://support.apple.com/en-au/guide/iphone/iphb8f1bf206/26/ios/26

Secondary and explanatory sources (used for conventions, worked examples, and figures not stated on a primary page; labelled as such where relied on):

- Material Design principles / whitespace: https://blog.adobe.com/en/publish/2015/05/12/new-to-material-design-12-principles-you-need-to-know
- Advanced Material Design techniques (grids, negative space): https://www.numberanalytics.com/blog/advanced-material-design-techniques
- Material Design 3 type scale and contrast floors (11sp label floor, 12sp body floor, 4.5:1 / 3:1, 200% scale): https://github.com/hamzaviour/whizzlylab/blob/HEAD/.agent/skills/android-design-guidelines/SKILL.md
- M3 foundations — contrast and colour roles: https://github.com/ephk15/design-guidelines/blob/HEAD/skills/m3-material/m3-foundations/SKILL.md
- Icon consistency (24dp grid, 2dp stroke/corner, live area): https://modus-v1.trimble.com/foundations/icon-definitions/ and https://dev.svgmaker.io/docs/tutorials/consistent-svg-app-icons-for-ios-android
- Themed icons — how they work and what breaks them: https://github.com/ichintansoni/skills-master/blob/HEAD/skills/android/design/technologies/m3-app-icons/SKILL.md and https://www.itechguides.com/android-13s-themed-icons-what-they-do-and-how-to-enable-them/
- Google Play icon specifications (512×512, no transparency, 30% radius applied by Google; no text in icons): https://tms-outsource.com/blog/posts/designing-an-icon-for-the-google-play-store/
- Notification badge design rules (99+ cap, pinned top-right, dot vs. number, clear on open, badge only decisions): https://www.threads.com/@designmotionhq/post/De_4ze5jPZo and https://www.instagram.com/reel/Dcnuc60NbeO/
- Home-screen layout — Glance / Action / Stable-access layers, first-page audit: https://pcnmobile.com/how-to-customize-your-android-home-screen-like-a-pro-in-2026/
- Pixel Launcher replication (5×5 grid, 5-icon dock, labels on, padding, Simple/None scroll): https://geekchamp.com/make-nova-launcher-look-and-work-like-pixel-launcher/
- Pixel Launcher guide (folders by drag-on-top, rename, drawer by swipe-up): https://blog.rottenwifi.com/pixel-launcher-everything-you-need-to-know-about-googles-pixel-home-screen/
- Long-term Pixel home screen (five dock icons, five folders, "boring, reliable"): https://www.androidauthority.com/why-i-stick-with-default-pixel-launcher-3616782/
- Home-screen basics (dock, drawer, widgets): https://www.howtogeek.com/345451/how-to-get-started-with-androids-home-screens/
- Android N folder style (circular window, clipped mini-icons): http://www.androidpolice.com/2016/04/13/android-n-dev-preview-2-includes-a-new-style-for-google-now-launcher-folders/
- Text over images — scrims, gradient easing, worst-pixel rule: https://www.shotcanvas.app/blog/text-over-images
- Contrast over photographs and gradients — worst pixel, scrim arithmetic: https://onlinetools.world/blog/contrast-over-photos-and-gradients/
- iOS Home Screen layout (6×4 grid + 4-slot Dock, iOS 26 icon styles): https://www.iscreenapp.com/blog/iphone-home-screen-layout-ideas
- iOS 18 Dark/Tinted icon versions (dark gradient, tinted as luminance image): https://medium.com/@GuillemBruix/design-your-dark-and-tinted-versions-of-your-app-icon-for-ios-18-827444a31851
- iOS app-icon reference (1024 master, squircle ~22.37% / ~60% smoothing, no pre-rounded corners, Icon Composer, Liquid Glass): https://github.com/app-builders-club/design-builder/blob/HEAD/skills/design/references/ios/icons.md
- iOS app-icon HIG specs (sizes incl. 180×180 @3x, opaque, sRGB/Display P3): https://github.com/mohamedabdallah-14/prompt-to-asset/blob/HEAD/docs/research/09-app-icon-generation/9a-ios-app-icon-hig-specs.md
- iOS widget sizes (HIG point sizes, e.g. 158×158 / 338×158 / 338×354 pt on 393×852 screens): https://github.com/ricsantos/ios-widget-sizes
- iOS widgets (small 2×2 / medium 4×2 / large 4×4, Smart Stack): https://prettyprogress.app/blog/home-screen-widgets-iphone
- HIG Widgets text (mirror, for the HIG wording on sizes and contexts): https://github.com/valentinllpz/apple-human-interface-guidelines/blob/HEAD/references/widgets.md
- Android vs. iOS home screens (launcher replacement, drawer vs. App Library, widgets, theming): https://webiano.digital/knowledge-base/mobile/mobile-fundamentals/home-screen/
- Stacked widgets — the iOS calm vs. Android haphazard comparison: https://www.androidauthority.com/android-vs-ios-stacked-widgets-3567106/
- iOS Home Screen customising (jiggle mode, free placement since iOS 18, folders, pages, labels off): https://solveyourphone.com/how-to-customize-iphone-home-screen-step-by-step-guide/
- iOS folder / page organisation (16–20 apps on page one + 4 dock slots; App Library categories): https://techetips.com/how-do-i-organize-my-apps-on-my-iphone-11/
- iOS 18 organisation (free placement, tinted icons, Large icons remove labels): https://techetips.com/how-do-i-organize-apps-with-the-new-iphone-os/
- New-app placement and badge settings on iOS: https://techsith.com/how-to-change-iphone-home-screen-layout/
- iOS badges (red, per-app switch, off in App Library by default): https://www.digitaltrends.com/tablets/how-to-control-notification-badges-on-iphone-and-ipad/ and https://www.solveyourtech.com/what-is-a-badge-app-icon-on-the-iphone-6/
- Apple HIG foundations distilled (clarity / deference / depth, safe areas, 17 pt body / 11 pt minimum, 4.5:1, SF Symbols, 44 pt targets): https://github.com/chrispiz/apple-app-ui-design/blob/HEAD/references/apple-hig.md and https://github.com/dodoxtech/ios-app-design-skills/blob/HEAD/skills/apple-hig-foundations/SKILL.md
- iOS vs. Android theming comparison (Liquid Glass, Material 3 Expressive, One UI): https://sites.google.com/view/stephanbtech/apples-ios-26-vs-googles-android-16-vs-samsungs-one-ui-7-a-visual-compa
- Android launcher theming / icon-pack contrast (ellipses, clutter, customisation as Android's strength): https://www.androidcentral.com/switching-android-android-theming-puts-apples-aesthetics-shame

*Note on source quality:* the primary sources above (developer.android.com, m3.material.io, developer.apple.com, support.apple.com, support.google.com) state the rules. The secondary sources state conventions, reported measurements, and worked examples; where a figure in this document comes only from a secondary source (e.g. the iPhone 6×4 grid, the ~22.37% squircle radius, the 54–60 dp logomark band, the 30–50% scrim range), the text says so or cites that source directly, so a reader can check it rather than taking it on trust.
