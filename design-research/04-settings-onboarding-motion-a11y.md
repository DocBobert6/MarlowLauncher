# 04 — Settings, Onboarding, Motion & Accessibility
## Marlow Launcher (`app.launcher`) — design research, track 4 of 4

**Scope.** Settings information architecture, onboarding / first-run, gesture and
motion design, and accessibility / legibility — for a Pixel-style Android home
launcher in Compose + Material 3. Research only; no code was touched.

**How this document is written — read this first.** This document makes no
claim about its author's employment, education, or qualifications, and it
contains no fabricated credentials of any kind — no claim of having worked at
Meta, Google, Apple, or on the original Android team, and no invented
qualifications. Every substantive claim below is tied to a real, publicly
available source, cited by its exact URL. Where a source was not consulted or
a detail (for example a talk speaker's name) could not be verified from the
sources read, that is stated as *not verified* rather than filled in. The
work stands on the sources, not on authority. The "insider-level" material in
§8 is insider-level only in the sense that it is drawn from the people who
built the platform — Google I/O talks, Android Developers documentation and
blog posts, Google's haptics writing, and published launcher engineering
writing — quoted and cited, not from any personal insider status.

**How to use it.** §1–§5 are principles. §6 is the numbered, checkable rules:
each rule is testable against the built app, and each carries the source that
justifies it. §7 is the proposed Marlow settings map (a proposal, derived
from the principles — not a Pixel fact). §8 is the expanded section: Google
I/O / Android developer talks and design-engineering writing on launcher
motion and haptics, with its own checkable rules. §9 is the top 10.

**Status note (from the project record, 2026-11-26).** Marlow was built and
shipped as `app.launcher` 0.1.0-dev, then audited; the user's verdict was that
it did not look or feel like a launcher that would actually come stock on a
phone. This track exists to close that gap. Nothing in this file is a claim
that Marlow already does any of this — the rules are the test it has to pass.

---

## Sources consulted (primary)

- Android Developers — Settings pattern:
  https://developer.android.com/design/ui/mobile/guides/patterns/settings
- AOSP — Android settings design guidelines:
  https://source.android.com/docs/core/settings/settings-guidelines
- Material Design 3 — Easing and duration:
  https://m3.material.io/styles/motion/easing-and-duration
- Material Design 3 — Easing and duration, tokens/specs:
  https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
- Material Design 3 — Motion overview / specs (M3 Expressive springs):
  https://m3.material.io/styles/motion/overview/specs
- Android Developers — Make apps more accessible:
  https://developer.android.com/guide/topics/ui/accessibility/apps?continue=https://developer.android.com/courses/pathways/make-your-android-app-accessible#article-https://developer.android.com/guide/topics/ui/accessibility/apps
- Android Developers — Request runtime permissions:
  https://developer.android.com/training/permissions/requesting?u=http://developer.android.com/guide/topics/renderscript/reference/rs_matrix
- Android Developers — Predictive back design:
  https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back?authuser=0
- Android Developers — Handle back gestures / predictive back:
  https://developer.android.com/guide/navigation/navigation-event/handle-back
- Android Developers — Add support for the predictive back gesture:
  https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture?authuser=5
- Android Developers — Ensure compatibility with gesture navigation:
  https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav?authuser=5
- AOSP — UX foundation for haptic framework:
  https://source.android.com/docs/core/interaction/haptics/haptics-ux-foundation?authuser=3
- Android Developers — Haptics design principles:
  https://developer.android.com/develop/ui/views/haptics/haptics-principles
- Android Developers — Implement haptics on Android:
  https://developer.android.com/develop/ui/views/haptics?hl=en
- Google (blog.google) — Haptic design: How Google builds and designs
  products to feel just right (15 Nov 2023):
  https://blog.google/products-and-platforms/devices/pixel/google-haptic-design/
- World Haptics Conference 2025 — Android Haptics: API and Implementation
  on Pixel (tutorial by Ahmad Khalil, Nathan Kulczak & Hong Tan):
  https://2025.worldhaptics.org/wp-content/uploads/2025/07/WHC2025_tutorial1.pdf
- Google Pixel Phone Help — Change or create a wallpaper on your Pixel phone:
  https://support.google.com/pixelphone/answer/14853283?hl=en&ref_topic=14852486
- Android Developers — ViewConfiguration API reference:
  https://Developer.android.com/reference/kotlin/android/view/ViewConfiguration
- Android Developers Blog — Get the big picture with Large Screens at
  Google I/O 2024 (Fahd Imtiaz, Product Manager, Android Developer):
  https://android-developers.googleblog.com/2024/05/get-big-picture-with-large-screens-at-google-io-24.html
- Android Developers Blog — Google I/O 2025: Build adaptive Android apps
  that shine across form factors:
  http://android-developers.googleblog.com/2025/05/adaptiveapps-io25.html
- Android Developers Blog — Building Premium Android Experiences at
  Google I/O '26 (Ataul Munim, Developer Relations Engineer):
  https://developer.android.com/blog/posts/building-premium-android-experiences-at-google-i-o-26
- Android Developers Blog — 17 Things to know for Android developers at
  Google I/O (2026):
  https://developer.android.com/blog/posts/17-things-to-know-for-android-developers-at-google-i-o
- Android Developers Blog — Building for the Intelligence System on Android:
  https://developer.android.com/blog/posts/building-for-the-intelligence-system-on-android
- Android Developers Blog — What's new for Android developers at Google I/O
  (20 May 2021, Karen Ng & Jacob Lehrbaum):
  https://developers.googleblog.com/whats-new-for-android-developers-at-google-io/
- 9to5Google — Watch all the Android 12 sessions at Google I/O 2021
  (incl. "Refreshing widgets" and "What's new in Android"):
  https://9to5google.com/2021/05/18/android-12-google-io/
- 9to5Google — 'New look' for Android 12 widgets, Wear, more Google I/O
  2021 sessions: https://9to5google.com/2021/04/28/google-io-2021-sessions/
- 9to5Google — Android 13 makes back gesture more predictable:
  https://9to5google.com/2022/05/11/android-13-back-gesture-predictable/
- Esper — How Android Gestures Will Be More Intuitive with the Predictive
  Back Gesture (Mishaal Rahman; with input from Kevin Barry, developer of
  Nova Launcher): https://www.esper.io/blog/android-dessert-bites-23-android-13-predictive-back-navigation-37581549
- XDA Developers — Here's why Nova Launcher works better on Google Pixel
  phones (QuickStep / third-party launcher gestures):
  https://www.xda-developers.com/third-party-launchers-animation-pixel-phones/
- How-To Geek — Gestures don't work well with Android launchers, and it's
  Google's fault: https://www.howtogeek.com/gestures-dont-work-well-with-android-launchers-and-its-googles-fault/
- Android Police — How to customize your Google Pixel 9 home screen:
  https://www.androidpolice.com/customize-google-pixel-9-home-screen
- Axo Launcher — How to Change Your Default Launcher on Android (2026):
  https://www.axolauncher.com/blog/how-to-change-default-launcher-android
- Nova Launcher settings structure (reviews / walkthroughs):
  https://www.aivanet.com/2015/02/monday-makeover-an-extensive-look-at-nova-launcher/ ;
  https://www.techjunkie.com/how-to-use-nova-launcher/ ;
  https://www.slashinfo.com/nova-launcher-review/
- Onboarding research synthesis: https://dbggr.dev/articles/user-onboarding-best-practices/
- Onboarding / permission prompts guide: https://www.creahive.com/guides/app-onboarding-ux/
- Android onboarding patterns: https://extensionbooster.net/blog/android-app-onboarding-ux-patterns-reduce-churn-guide/
- Accessibility, text scaling & focus order (Compose):
  https://www.thedroidsonroids.com/blog/mobile-app-accessibility-android-guide-part-2
- Reduce motion / animation scale:
  https://github.com/almasumdev/awesome-mobile-accessibility-agent-skills/blob/HEAD/.github/skills/motion/reduce-motion/SKILL.md
- Android Central — How to find what you're looking for in the Android
  settings menu: https://www.androidcentral.com/how-find-what-youre-looking-android-settings-menu
- Google I/O 2017 — What's new in notifications, launcher icons, and
  shortcuts (Class Central listing):
  https://www.classcentral.com/course/youtube-what-s-new-in-notifications-launcher-icons-and-shortcuts-google-i-o-17-211952
- Google I/O 2022 — "Back to the basics of System Back" video (linked from
  a session recap): https://www.youtube.com/watch?v=Elpqr5xpLxQ
- Google I/O 2021 — "Refreshing widgets" video:
  https://www.youtube.com/embed/cHv84zXZf6E?feature=oembed&rel=0&enablejsapi=1
- Google I/O 2021 — "What's new in Android" video:
  https://www.youtube.com/embed/D2cU_itNDAI?feature=oembed&rel=0&enablejsapi=1

---

## 1. Principles — settings information architecture

### 1.1 The stock launcher has almost no settings. That is the point.
Pixel Launcher's own surface is tiny. The user reaches it by long-pressing
empty home-screen space, which offers **Wallpaper & style**, **Home
settings**, and **Widgets** — not a sprawling settings app. Wallpaper &
style holds wallpaper, colours, themed icons, icon shape and the app grid /
layout; Home settings holds suggestions and search behaviour (and At a Glance
via its own long-press). Source: Pixel help
(https://support.google.com/pixelphone/answer/14853283?hl=en&ref_topic=14852486)
and Android Police Pixel 9 walkthrough
(https://www.androidpolice.com/customize-google-pixel-9-home-screen).
A stock-feeling launcher therefore earns trust by *omission*: if a setting
is not something a normal person would ever change, it does not exist.

### 1.2 Nova is the anti-model for IA — useful as a warning, not a template.
Nova Settings is organised as Desktop, App & Widget Drawers, Dock, Folders,
Look & Feel, Night Mode, Gestures & Inputs, Notification Badges, Backup &
Import — each itself deep, with grid, scroll effects, icon layout, drawer
styles, dock pages, folder styles, animation speed and scroll speed all
exposed. Sources:
https://www.aivanet.com/2015/02/monday-makeover-an-extensive-look-at-nova-launcher/ ;
https://www.techjunkie.com/how-to-use-nova-launcher/ ;
https://www.slashinfo.com/nova-launcher-review/
Nova proves that a launcher *can* expose everything; it also proves why a
stock-feeling launcher must not. Marlow should borrow Nova's *grouping
vocabulary* (Desktop / Drawer / Dock / Folders / Look & feel / Gestures are
intuitive nouns) while keeping Nova's *depth* out: one level of sub-screens,
few items per screen, no "Advanced" dumping ground with 20 toggles.

### 1.3 The official rules, in one paragraph.
Android's own settings guidance says: respect system settings (your app may
not need its own); use clear and precise language; provide an overview and
divide settings by groups and screens; use appropriate selection patterns;
use polite defaults; keep settings accessible. Include infrequently accessed
preferences; do *not* include frequently accessed actions (those belong in
context, next to the feature); save preferences; avoid app info / version /
licensing in settings; avoid account management; do not replicate device-level
preferences. Source:
https://developer.android.com/design/ui/mobile/guides/patterns/settings
AOSP adds: a user should be able to glance at a screen and understand every
setting and its value; put frequently used settings at the top; **more than
10–15 items on one screen is overwhelming** — move some to a separate screen;
avoid vague titles like "General settings"; show the *status* (the actual
value) under the title, not a description of the title. Source:
https://source.android.com/docs/core/settings/settings-guidelines

### 1.4 Setting vs status vs action — three different rows.
- A **setting** is a preference the user changes (a switch, a choice, a
  slider). Its row shows its current value.
- A **status line** is information, not a control: "Home app: Marlow",
  "Version 0.1.0-dev", "Storage used". It is read-only, it never looks like a
  toggle, and it belongs at the bottom of a screen or on an About screen —
  never mixed into the middle of toggles, where it reads as a broken control.
  AOSP: "Below the title, show the status to highlight the value of the
  setting. Show the specific details instead of just describing the title."
  (https://source.android.com/docs/core/settings/settings-guidelines) and
  Android Developers: "Avoid information about the app, such as a version
  number or licensing information in settings."
  (https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- An **action** (Reset layout, Back up, Restore, Clear search history) is a
  one-off verb. It has no on/off state, no switch, and destructive actions
  are separated, grey/negative, and confirmed — never placed where a slip
  of the thumb can fire them.

### 1.5 Wording rules.
Labels are brief, meaningful, most-important-word-first; no generic verbs
(Set / Change / Edit / Modify / Manage / Use / Select / Choose); no
repeating the section title's words; no technical jargon unless widely
understood; no negative words ("Don't", "Never") — rephrase neutrally
("Block"); impersonal labels ("Notifications", not "Notify me"); second
person ("you") only when referring to the user is necessary, never first
person ("I"). Supporting text shows the *current state* ("5 × 4") instead of
describing the setting, and is omitted entirely when the label alone is
sufficient. Sources:
https://developer.android.com/design/ui/mobile/guides/patterns/settings ;
https://source.android.com/docs/core/settings/settings-guidelines
Sentence case everywhere (Material writing convention); buttons are
Verb + Object ("Reset home screen", not "Reset" alone). No internal jargon,
no ticket numbers, no developer asides, no plan/feature numbers in any
user-visible string — the audit that preceded this research stripped exactly
that class of string from Marlow, and it must not creep back.

### 1.6 Surface vs buried.
- **Surface (top level, or in-context):** the 5–8 things a user changes in
  the first week — grid, wallpaper/style entry, themed icons, notification
  dots, suggestions on/off, search behaviour, gestures on/off. AOSP: put
  frequently used settings at the top.
- **One sub-screen deep:** a coherent group (Drawer, Dock, Folders,
  Gestures, Appearance) — reached from the overview, with a title that
  matches the entry label exactly ("the label of the setting that opens a
  group must match the subscreen title").
  Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
- **Buried / "Advanced" only:** things almost nobody changes. AOSP: use
  "Advanced" only when there are at least 3 items to hide, and its subtext
  lists the hidden titles on one line, truncated with an ellipsis.
  Source: https://source.android.com/docs/core/settings/settings-guidelines
- **Not in settings at all:** frequently used actions (add widget, change
  wallpaper, edit a folder) belong in context — long-press on the thing
  itself. Android Developers: "Don't include frequently accessed actions.
  These should be contextual to the feature they most affect."
  (https://developer.android.com/design/ui/mobile/guides/patterns/settings)

### 1.7 Defaults that feel stock ("polite defaults").
A default must be what most users would choose, neutral and low-risk, low
on battery/data, and interrupting only when important. Source:
https://developer.android.com/design/ui/mobile/guides/patterns/settings
Applied to a launcher, that means: the out-of-box home screen looks like a
Pixel (grid, dock, search, At a Glance-style strip, wallpaper-driven colour),
labels on, notification dots on, suggestions on (Pixel ships them on, and
offers a plain off switch), themed icons off (Pixel's is an opt-in toggle in
Wallpaper & style), swipe-up-to-open-drawer on, rotation locked to portrait
on phones, haptics on at system strength, no experimental feature on by
default. Every extra/Pro feature off by default is the project's own standing
toggle rule and agrees with the polite-defaults rule; the two must never
conflict in the shipped strings.

### 1.8 Search over settings.
For 15+ settings, group under sub-screens *and* add search, so users can find
a preference without knowing where it lives. Source:
https://developer.android.com/design/ui/mobile/guides/patterns/settings
AOSP's search-results pattern: each result shows title, subtext (if any),
and the **breadcrumb location** of the setting — the path is part of the
result, because it teaches the user where the setting lives.
Source: https://source.android.com/docs/core/settings/settings-guidelines
Behaviour that follows: search is live as you type; it matches title,
synonyms and effect words ("loud" → volume, "dark" → dark theme), not only
exact labels; tapping a result jumps *directly* to that setting (deep link
into the sub-screen, with the row highlighted), it does not merely open the
top level; an empty result is an honest empty state ("No settings match
'…'"), not a dead list. See also Android Central's settings-search guide:
https://www.androidcentral.com/how-find-what-youre-looking-android-settings-menu

---

## 2. Principles — onboarding / first-run

### 2.1 What a stock first run looks like.
Pixel's model is not a launcher tutorial — it is the *device* setup flow,
and the launcher inherits its result. The launcher's own first run, in the
stock pattern, is therefore almost nothing: the home screen simply appears,
populated, and the user learns by using it. Third-party launchers that do
have a first run (Nova, Action Launcher) ask, in order: start fresh or
import a layout; set as default home; then get out of the way. Sources:
https://macmyths.com/how-to-make-nova-launcher-look-and-function-like-the-pixel-launcher/ (cited in earlier track research; Nova first-run: fresh vs import) and
https://www.axolauncher.com/blog/how-to-change-default-launcher-android
("Since Android 10, a launcher can ask to become your home app itself …
follow its setup, and Android shows a prompt: 'Set [launcher] as your
default home app?'").

### 2.2 What it asks, in what order — and what it never asks.
1. **Nothing before value.** Show the home screen first. The onboarding
   literature is consistent: value first, everything else later; teach by
   doing, not by slides; prefer tips in context over one long flow at the
   start. Sources: https://dbggr.dev/articles/user-onboarding-best-practices/
   ("Skip the up-front tour unless your product is complex or unfamiliar …
   teach in context … Choose defaults carefully, because most people keep
   them"); https://www.creahive.com/guides/app-onboarding-ux/ (Apple's
   guidance as summarised there: "ideally people understand an app simply by
   using it … Keep any required flow brief"); https://extensionbooster.net/blog/android-app-onboarding-ux-patterns-reduce-churn-guide/
   ("show value before asking for anything … Permission priming — ask once,
   ask right").
2. **Set as default home — one step, at the moment it matters.** This is the
   only first-run question a launcher genuinely needs, and it is the one
   Android itself provides a system prompt for. The launcher triggers the
   system "Set [launcher] as your default home app?" prompt; the user taps
   "Set as default" and the home screen opens. If declined once, it can come
   back with a "Don't ask again" box — tick that and the launcher can no
   longer ask, so the Settings route
   (Settings → Apps → Default apps → Home app) must be offered instead, in
   plain words, from inside Marlow Settings. Source:
   https://www.axolauncher.com/blog/how-to-change-default-launcher-android
3. **Defaults are presented, not chosen.** The first run shows the stock
   home screen *already* in its default state; it does not present a
   questionnaire of grid sizes and toggles. A choice is offered only where
   there is no safe default — and for a launcher there is almost nowhere
   like that. Research note: in a 2005 study of Facebook cited in the
   onboarding synthesis, only 1.2% of users had changed a default privacy
   setting — defaults are decisions, not placeholders.
   Source: https://dbggr.dev/articles/user-onboarding-best-practices/
4. **What it never asks:** no account, no sign-in (a launcher has no
   account), no email, no phone number, no notification permission up front,
   no contacts permission up front, no "rate us", no feature tour of more
   than 3 steps, no "choose your theme" interrogation before the home screen
   has even appeared. Each of those is friction at the exact moment users
   are most likely to abandon, and none is needed for the first useful
   moment — which for a launcher is simply *being home*.

### 2.3 How many steps is too many.
Three is the honest ceiling for a launcher first run, and one is better:
(1) welcome / "this is your home screen" (skippable, one screen),
(2) set as default home (system prompt), (3) done — home screen. Anything
beyond that is a tour, and tours help only for complex or unfamiliar
products; in a randomised test of 45,000+ new players cited in the
onboarding synthesis, tutorials helped only in the most complex of three
games and did nothing or slightly harmed return rates in the simpler two.
A launcher is the simple case. Source:
https://dbggr.dev/articles/user-onboarding-best-practices/

### 2.4 Permission asks — when, how, in what order, what happens if declined.
Android's own rule: ask for a permission **in context, when the user starts
to interact with the feature that requires it**; don't block the user; always
provide a way to cancel an educational/rationale flow; if denied, gracefully
degrade — disable that feature, keep the rest of the app working. Never
assume system behaviour (e.g. permission groups). Source:
https://developer.android.com/training/permissions/requesting?u=http://developer.android.com/guide/topics/renderscript/reference/rs_matrix
For Marlow specifically: there is almost nothing to ask for at first run.
The real permission moments are all deferred and contextual —
- **Notifications** (POST_NOTIFICATIONS, Android 13+): only when the user
  turns on a feature that needs it (e.g. a Now Brief / morning delivery, if
  shipped); priming screen first (what you get, in one sentence), then the
  system dialog, only if the user opts in on the priming screen. If declined
  on the priming screen, do not fire the system dialog — a hard deny is
  treated by Android as a permanent refusal after repeated denies, and the
  user then has to go to Settings. Source:
  https://extensionbooster.net/blog/android-app-onboarding-ux-patterns-reduce-churn-guide/ ;
  https://www.creahive.com/guides/app-onboarding-ux/
- **Contacts** (for a contacts shortcut / folder): only when the user adds
  one; declined → the shortcut simply isn't created, with a one-line
  explanation and a "try again" path, not a blocking error.
- **Notification-listener / accessibility / device-admin** (badges, screen
  lock by gesture, etc.): only when the user enables that exact feature, via
  the system Settings screen Android requires — never as a first-run bundle.
- **Storage / photos** (wallpaper picker): only when the user opens the
  wallpaper picker and chooses "your photos"; the bundled wallpapers need no
  permission at all.
A stated reason raises grant odds ~2.7× and asking in context ~1.5× in the
controlled study cited in the onboarding synthesis — so every ask carries
its reason in the priming line, not in a paragraph.
Source: https://dbggr.dev/articles/user-onboarding-best-practices/
Decline is never punished: no repeated re-prompt on every launch, no
degraded home screen, no "you must enable X to continue". The feature is
off; the rest works; the toggle that needs it explains, in its own subtext,
what enabling it unlocks and where to enable it.

---

## 3. Principles — gesture and motion

### 3.1 The vocabulary of a launcher is small. Master it, don't extend it.
Swipe between home pages; swipe up (from dock / anywhere, per Pixel) to open
the drawer; swipe down to close it; long-press an icon for its menu
(shortcuts, app info, widgets); long-press empty space for Wallpaper &
style / Home settings / Widgets; drag to move; drag onto another icon to
make a folder; tap a folder to open it, tap outside / back to close it;
swipe up-and-hold for Overview/Recents (system gesture, owned by QuickStep
— see §8.4); swipe from an edge for Back (system gesture, predictive — see
§8.1). Every gesture must do what a Pixel does, in the same place, with the
same timing. Novel gestures are a cost, not a feature.

### 3.2 Long-press: timing is not a design choice.
The platform defines it: `ViewConfiguration.getLongPressTimeout()` is
**500 ms** (TAP_TIMEOUT 115 ms, DOUBLE_TAP_TIMEOUT 300 ms). Source:
https://Developer.android.com/reference/kotlin/android/view/ViewConfiguration
and the AOSP source as quoted in search results (LONG_PRESS_TIMEOUT = 500).
Do not invent a shorter or longer threshold: a custom threshold fights the
user's muscle memory and, worse, fights their Accessibility setting —
long-press timeout is user-adjustable in
Settings → Accessibility → Interaction controls → Long press timeout
(Short 400 ms / Medium 1000 ms / Long 1500 ms in the documented values), and
the framework respects that preference. The correct implementation reads
the system value; a hard-coded 300 ms or 800 ms is a bug, not a tuning
choice. Haptic fires *at* the long-press moment, not on lift: one crisp
`LONG_PRESS` constant, so the finger feels the menu arrive. Drag starts
only after that haptic; before it, the icon must not move.

### 3.3 Motion must feel physical: easing, duration, follow-through.
Material 3's rule, verbatim in spirit: objects don't start or stop
instantaneously; transitions without easing look stiff and mechanical.
Emphasized easing is recommended for most transitions; Standard for small
utility transitions. The sanctioned pairs are:
Emphasized 500 ms — begin and end on screen; Emphasized decelerate 400 ms —
enter the screen; Emphasized accelerate 200 ms — exit the screen;
Standard 300 ms — begin and end on screen (utility); Standard decelerate
250 ms — enter (utility); Standard accelerate 200 ms — exit (utility).
Duration scales with the size/distance of the transition; exits are shorter
than entrances because they need less attention. Source:
https://m3.material.io/styles/motion/easing-and-duration
Tokens (short 50/100/150/200 ms; medium 250/300/350/400 ms; long 450/500/
550/600 ms; extra-long 700–1000 ms) are at:
https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
M3 Expressive (2025) adds a second system — spring physics for components
(stiffness, damping, slight overshoot and settle) — while the easing/duration
system remains the one for transitions. Source:
https://m3.material.io/styles/motion/overview/specs
Follow-through is choreography, not decoration: the container moves first,
its contents settle slightly after (stagger ~30–60 ms); a folder opens as a
container transform from its icon (the icon *is* the folder, grown), not as
an unrelated sheet sliding in; a drawer opens with the page content receding
slightly, not by teleporting. Transitions that exceed ~400–500 ms for a
single step feel slow; micro-interactions live at 100–200 ms. Never animate
layout properties (width/height/top/left) — transform and opacity only —
or frames drop under real home-screen weight.

### 3.4 Haptics: less is more, and the constants are the vocabulary.
Android's haptics guidance, in one line: "less is more. Too much vibration
can be annoying and even numbing to the hands … However, well crafted
haptics provide valuable sensory feedback." Source:
https://developer.android.com/develop/ui/views/haptics/haptics-principles
The classification is clear / rich / buzzy: clear haptics are crisp,
single-impulse sensations for discrete events (a button press, a toggle);
rich haptics sequence clear primitives for texture, drag, wobble; buzzy
haptics are noisy, ringing, tingling — the pager-era failure mode — and are
to be avoided. Prefer predefined constants (`HapticFeedbackConstants`) over
raw vibration so the platform can fall back gracefully per device; correlate
strength with importance × frequency (frequent events like scrolling or
text-handle moves get the subtlest tick; important, rare events like a
successful drop or a confirm get stronger); co-design haptics with the
visual animation so they land on the same frame — out-of-sync haptics read
as a broken actuator. Never use legacy one-shot `vibrate(long)` — a good
keyclick is 10–20 ms, and the actuator can ring 20–50 ms after a 20 ms
input. Sources:
https://developer.android.com/develop/ui/views/haptics/haptics-principles ;
https://source.android.com/docs/core/interaction/haptics/haptics-ux-foundation?authuser=3
Google's own Pixel haptics work (blog.google, 15 Nov 2023) adds the human
detail: the team moved away from the 200–300 Hz band where skin is most
sensitive ("like a soprano voice — very sharp, not everyone appreciates
it", lead haptics scientist Hong Tan, as quoted there) toward rounder,
click-like and gentle variants — "click" (very short), "tick" (less intense),
"low tick" (softer, lower frequency, more organic) — tested in their own
playground app, and with the explicit aim that haptics "alert, inform and
delight". Source:
https://blog.google/products-and-platforms/devices/pixel/google-haptic-design/
Launcher haptic map that follows from all of this (see rules G13–G20):
long-press pickup, drag snap/drop, page snap, drawer/folder open/close
threshold, toggle flip — yes, each once, crisp. Tap on an icon, scroll,
swipe between pages, typing in search — no haptic, or the subtlest tick at
most. Errors get *no* haptic (visual only). Never vibrate on a loop, never
vibrate to announce a notification the system already announced.

### 3.5 Overview / Recents is not Marlow's animation to own.
On modern Android the Recents/Overview transition is owned by the system
launcher's QuickStep component. A third-party launcher cannot reproduce the
Pixel's app-to-icon minimise animation by itself — it can only cooperate
with the system's gesture (see §8.4). The honest rule for Marlow: do not
fake a Recents screen, do not intercept the swipe-up-and-hold gesture, and
judge the home transition by whether the app window shrinks *toward its
icon* rather than fading or sliding. If that animation is janky, the cause
is the QuickStep boundary, not a duration constant — and the fix is not a
custom interpolator.

---

## 4. Principles — accessibility and legibility

### 4.1 Touch targets: 48 × 48 dp is a floor, not a goal.
"For touch interfaces, we recommend that each interactive UI element have a
focusable area, or touch target size, of at least 48dp × 48dp. Larger is
even better. In Jetpack Compose, many built-in Material components like
`Button`, `IconButton`, and `ListItem` already enforce this minimum size."
Source:
https://developer.android.com/guide/topics/ui/accessibility/apps?continue=https://developer.android.com/courses/pathways/make-your-android-app-accessible#article-https://developer.android.com/guide/topics/ui/accessibility/apps
For a launcher this bites hardest where icons are small and dense: the
page-indicator dots, the drawer close affordance, folder close, the search
bar's mic/lens icons, widget resize handles, the "X" on a badge, and every
row in Settings. A visually 24 dp icon inside a 48 dp hit area passes; a
visually 48 dp icon with a 32 dp hit area fails. Spacing between targets
matters too (8 dp is the working separation in the Material-derived guidance
— mis-taps come from crowding as much as from small targets).

### 4.2 Contrast over wallpapers is the launcher's hardest legibility problem.
The thresholds are fixed: text smaller than 18 sp (or bold smaller than
14 sp) needs **4.5:1**; all other text needs **3:1**. Source: same Android
accessibility page as §4.1. A launcher cannot control the wallpaper, so it
cannot guarantee contrast by choosing a text colour — it must guarantee it
by construction: a scrim / shadow / tonal surface behind icon labels, the
drawer, the search bar and At a Glance text, so that the *effective*
background behind the text always clears the ratio. White text directly on
a bright photo wallpaper fails; white text on a scrim over that wallpaper
passes. Test with the lightest and the busiest bundled wallpapers, in light
and dark theme, and with dynamic colour on. Never convey meaning by colour
alone (a selected page dot needs shape/position as well as colour; a badge
needs a count, not just a red dot).

### 4.3 Dynamic text scaling: sp, never dp; no fixed heights.
All text in `sp` so the system font scale applies; never hard-code `dp` for
text sizes and never pin `fontScale = 1f`. WCAG resize-text expectation is
up to **200%** without loss of content or functionality; Android has
supported non-linear font scaling to 200% since Android 14. Layouts must
survive it: no fixed-height containers that clip at 200%, text wraps or
scrolls instead of truncating silently, and an ellipsis where truncation is
unavoidable (TalkBack still reads the full text). Source:
https://www.thedroidsonroids.com/blog/mobile-app-accessibility-android-guide-part-2
Test at fontScale 2.0 in preview and on device; the failure mode is always
the same — a label that was fine at 100% is cut off or overlaps its
neighbour at 200%.

### 4.4 TalkBack structure: the home screen is a list, not a picture.
TalkBack traverses an accessibility tree, not pixels. Every meaningful
element needs a label, a role and a value; decorative elements are hidden
from the tree. Concretely for a launcher: every icon-only button has a
`contentDescription` naming its *action* ("Settings", not "gear icon" —
TalkBack already announces the role, so don't write "Settings button");
app icons announce app name + position ("Chrome, page 1, position 3") and,
where relevant, badge count; folders announce name + count ("Social, folder,
6 apps"); the drawer search field has a hint, not a contentDescription;
state is exposed via `stateDescription` (selected / checked / expanded),
not by colour; swipe-only actions (swipe to delete, drag to move) always
have a `customActions` equivalent, because TalkBack cannot swipe; focus
order is logical (left-to-right, top-to-bottom; home pages before dock;
drawer list in reading order) and focus is managed on dialogs, bottom
sheets and the folder popup (focus enters the popup, returns to the folder
icon on dismiss). Use native Compose controls wherever possible — they
carry role and state for free — and only add a semantics layer where the
component is genuinely bespoke. Sources: the Android accessibility page
(§4.1) and
https://www.thedroidsonroids.com/blog/mobile-app-accessibility-android-guide-part-2

### 4.5 One-handed reachability.
The home screen's most-used actions must be reachable with one thumb: the
dock, the drawer handle, search, and back. That is why the drawer opens by
swiping up from the bottom (thumb territory) and why Pixel puts search at
the bottom of the home screen and at the bottom of the drawer. Settings rows
and destructive actions sit where a thumb does not land accidentally.
Gesture insets matter: never place a tap target fully inside the system
gesture inset zones (screen edges for back, bottom for home) — predictive
back respects those insets, and a control placed under them is unreachable
or, worse, triggers back instead. Source:
https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back?authuser=0
("Avoid adding touch gestures or drag targets under these gesture areas.")

### 4.6 Motion sensitivity is an accessibility feature, not a preference.
Android 12+ has Settings → Accessibility → Colour and motion → **Remove
animations**; Developer options has the three animation scales (Window /
Transition / Animator duration scale) which can be set to Off. When the
user has animations off, Marlow must honour it: replace slide/zoom/parallax
with a crossfade or no motion, keep opacity-only changes, keep essential
feedback (a progress indicator) but drop decorative travel. Never exceed 3
flashes per second (WCAG 2.3.1, photosensitive epilepsy). Source:
https://github.com/almasumdev/awesome-mobile-accessibility-agent-skills/blob/HEAD/.github/skills/motion/reduce-motion/SKILL.md

---

## 5. Principles — what "stock" feels like, in one paragraph

A stock launcher feels inevitable because nothing surprises: the home screen
is already populated; the grid is regular; the dock is five icons; the
search bar is where the thumb expects it; long-press does the same thing
everywhere; the drawer opens the same way every time; settings are few,
plainly worded, and show their values; the first run asks one question; a
declined permission changes one feature, not the phone; motion is quick,
soft-landing, and slightly springy; haptics are rare and crisp; text is
readable on any wallpaper; and a screen reader can use the whole thing.
Every rule in §6 is a test for one clause of that paragraph.

---

## 6. CHECKABLE RULES

*How to read them: each rule is numbered, states a pass/fail test, and names
the source that justifies it. "Test" is how to check it on the device or in
the build. Rules are grouped: S = settings IA, O = onboarding, G = gesture
& motion, A = accessibility. A rule marked [PROPOSAL] is Marlow-specific
judgement derived from a cited principle, not a fact about Pixel — it is
labelled so it can be challenged without disturbing the sourced rules.*

### 6.1 Settings information architecture

**S1 — Top level has 5–7 sections, not 10+.**
Test: open Marlow Settings; count top-level rows. Pass if 5–7.
Source: AOSP 10–15 items per screen is overwhelming; Pixel's own surface
is Wallpaper & style / Home settings / Widgets, not a catalogue.
https://source.android.com/docs/core/settings/settings-guidelines ;
https://support.google.com/pixelphone/answer/14853283?hl=en&ref_topic=14852486

**S2 — No single screen shows more than 15 rows.**
Test: scroll every settings screen; count rows. Pass if ≤15 on each.
Source: https://source.android.com/docs/core/settings/settings-guidelines
("Showing more than 10-15 items can be overwhelming.")

**S3 — Every sub-screen title matches its entry label exactly.**
Test: tap each top-level row; compare the sub-screen title with the row
label character for character. Pass if identical.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("the label of the setting that opens a group must match the subscreen
title.")

**S4 — No vague section titles.**
Test: grep all settings strings for "General", "Misc", "Other settings",
"Advanced settings" as a top-level title. Pass if none appears as a section
title. ("Advanced" is allowed only as the AOSP progressive-disclosure row —
see S9.)
Source: https://source.android.com/docs/core/settings/settings-guidelines
("Avoid using vague titles like 'General settings.'")

**S5 — Every row shows its current value as supporting text.**
Test: for each setting row on the overview, read its subtext. Pass if it
states the value ("5 × 4", "On", "System default") rather than describing
the setting ("Change the grid size").
Source: https://source.android.com/docs/core/settings/settings-guidelines
("Below the title, show the status … Show the specific details instead of
just describing the title.")

**S6 — No setting row repeats the section title's words.**
Test: within each section, check row labels do not repeat the section name
(e.g. under "Drawer", no row called "Drawer grid"). Pass if none does.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Don't write labels that … Repeat words from the section title.")

**S7 — No generic-verb labels.**
Test: grep settings strings for labels beginning Set / Change / Edit /
Modify / Manage / Use / Select / Choose. Pass if none.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings

**S8 — No negative labels; no first-person labels.**
Test: grep for "Don't", "Never", "Notify me", "My …" in settings labels.
Pass if none; neutral rephrasing ("Block", "Notifications") instead.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings ;
https://source.android.com/docs/core/settings/settings-guidelines

**S9 — "Advanced" only when ≥3 items are hidden, and its subtext lists them.**
Test: if an "Advanced" row exists, count the items it hides (≥3) and read
its subtext — one line, the hidden titles, truncated with an ellipsis.
Pass on both counts.
Source: https://source.android.com/docs/core/settings/settings-guidelines
("Use 'Advanced' only when there are at least 3 items to hide … subtext
should be only one line.")

**S10 — The entry point is exactly "Settings", in the long-press menu and nowhere else as a synonym.**
Test: long-press empty space → the menu offers "Home settings"; no row or
menu anywhere says "Options", "Preferences" or "Launcher options".
Pass if only "Settings" / "Home settings" appear.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Don't use synonyms such as 'Options' or 'Preferences.'")

**S11 — Frequently used actions live in context, not in Settings.**
Test: adding a widget, changing wallpaper, renaming a folder, removing an
app from home are all reachable by long-press on the thing itself, without
opening Settings. Pass if all four are.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Don't include frequently accessed actions. These should be contextual.")

**S12 — No device-level preference is duplicated.**
Test: Marlow has no own toggle for system dark theme scheduling, system
font size, system animation scale, or system notification sound. Where
Marlow extends the system (theme choice), it says so and defers to the
system default as the default. Pass if no duplicate exists.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Avoid replicating preferences available at the device settings level …
Don't override settings provided by the system.")

**S13 — App info (version, licences, storage) is not mixed into settings rows.**
Test: version / licence / "About" lives on its own About screen at the
bottom of Settings, as status lines, not as toggles and not between
preference rows. Pass if no version string appears among preference rows.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Avoid information about the app, such as a version number or licensing
information in settings.")

**S14 — A setting and a status line are visually distinct.**
Test: on every screen, a read-only status line (e.g. "Home app: Marlow")
has no switch, no chevron, no ripple that implies it is tappable; a
setting has a control or a chevron. Pass if a user cannot mistake one for
the other.
Source: https://source.android.com/docs/core/settings/settings-guidelines
(title + status pattern) — derivation; the distinction itself is the
setting-vs-status principle in §1.4.

**S15 — A dependent setting sits directly below its parent, disabled with a reason.**
Test: turn off a parent (e.g. Suggestions); its children grey out in
place, with one line explaining why ("Turn on Suggestions to change
this"), rather than disappearing or staying tappable. Pass if the
explanation is present.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
(Dependency pattern: "Place a dependent setting below the setting on which
it depends, with a brief explanation of why the dependent setting is
unavailable.")

**S16 — A feature that can be turned off wholesale has one master switch at the top of its own screen.**
Test: open the Suggestions screen (or equivalent); the top row is a single
switch for the whole feature, and turning it off disables all its children.
Pass if present.
Source: https://source.android.com/docs/core/settings/settings-guidelines
(Main setting pattern: "Using the main setting to disable the feature
disables all other related settings.")

**S17 — Single-choice settings use radio on a sub-screen/dialog, never radio rows in a list; single on/off uses a switch, never a radio.**
Test: grid size / icon shape / theme choice open a radio sub-screen or
dialog; no radio buttons appear inline in a settings list; no boolean
setting is presented as a radio pair. Pass if both hold.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Don't use radio buttons for settings with a single option … Present radio
buttons in a dialog or child screen."); https://source.android.com/docs/core/settings/settings-guidelines
("Radio buttons should not be used in settings list.")

**S18 — Switches over checkboxes.**
Test: count checkboxes in Settings. Pass if zero, except for a genuine
negative-action list (block/restrict) where the AOSP exception applies —
and then it is documented in code review, not silent.
Source: https://source.android.com/docs/core/settings/settings-guidelines
("Use switches over checkboxes when possible.")

**S19 — Toggles apply immediately; there is no Save button in Settings.**
Test: flip any toggle; leave the screen; return. Pass if the change
persisted with no save/apply step. (A choice that needs Apply — e.g. grid
size with a preview — is the documented exception, and its button says
"Apply".)
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Do save user preferences.") — switch semantics in Material settings.

**S20 — Search over settings exists, is live, and deep-links.**
Test: in Settings, type "grid" / "dots" / "dark" (including a synonym the
label doesn't contain). Pass if: results filter as you type; each result
shows its breadcrumb path ("Home settings › Drawer"); tapping a result
lands on that setting's sub-screen with the row visible/highlighted; a
no-match query shows an honest empty state, not a blank list.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("For 15 or more settings … add search functionality"); https://source.android.com/docs/core/settings/settings-guidelines
("Search results show the title, subtext (if available), and the breadcrumb
location of the setting.")

**S21 — Settings is reachable in ≤2 taps from the home screen, and from the drawer.**
Test: from home, long-press empty space → "Home settings" (2 taps). From
the drawer, the settings entry is present (long-press menu or drawer
overflow). Pass if both routes work.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Provide clear and access to settings.")

**S22 — Stock defaults: labels on, dots on, suggestions on, themed icons off, swipe-up drawer on, portrait locked on phones.** [PROPOSAL — derived from the polite-defaults rule + Pixel behaviour]
Test: fresh install, before any change: icon labels visible; notification
dots on; suggestions on; themed icons off; swipe-up opens the drawer;
home does not rotate on a phone. Pass if all six hold.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Choose common defaults … Represent the default most users would choose …
Be neutral and pose little risk"); Pixel behaviour:
https://www.androidpolice.com/customize-google-pixel-9-home-screen
(themed icons is an opt-in toggle in Wallpaper & style).

**S23 — Every extra / Pro feature is off by default.** [PROPOSAL — project standing rule, consistent with S22]
Test: fresh install; enumerate every non-stock feature flag. Pass if all
are off.
Source: project decision (2026-11-26 plan) + polite-defaults rule above.

**S24 — No user-visible string contains a plan/feature number, a ticket number, or a developer aside.**
Test: grep all string resources for digits in feature-number patterns,
"TODO", "FIXME", "dev:", internal codenames. Pass if none in user-visible
strings.
Source: wording rules in §1.5 (no technical jargon; plain language) —
this is the Marlow-specific enforcement of them, from the 2026-11-26
audit that stripped exactly these strings.

### 6.2 Onboarding / first-run

**O1 — The home screen appears before any question is asked.**
Test: fresh install → launch. Pass if the first thing seen is a populated
home screen, not a setup wizard, permission dialog, or sign-in.
Source: https://dbggr.dev/articles/user-onboarding-best-practices/
("show value before asking for anything"); https://www.creahive.com/guides/app-onboarding-ux/

**O2 — First run is ≤3 steps, and step 1 is skippable.**
Test: count first-run screens. Pass if ≤3, each with a visible Skip / Not
now, and completing or skipping lands on the home screen.
Source: https://dbggr.dev/articles/user-onboarding-best-practices/
(tours help only for complex products; a launcher is the simple case).

**O3 — "Set as default home" uses the system prompt, once, at the right moment.**
Test: fresh install → the first-run flow triggers Android's "Set Marlow as
your default home app?" prompt; tapping "Set as default" opens Marlow as
home. Pass if the system prompt (not a custom imitation) is used.
Source: https://www.axolauncher.com/blog/how-to-change-default-launcher-android
("Since Android 10, a launcher can ask to become your home app itself …
Android shows a prompt: 'Set [launcher] as your default home app?'")

**O4 — After a decline, Marlow never re-prompts on launch; it offers the Settings route instead.**
Test: decline the default-home prompt (including ticking "Don't ask again"
if offered); relaunch Marlow three times. Pass if no prompt reappears, and
Settings contains a plain row ("Home app: not Marlow — Change in system
settings") that opens Settings → Apps → Default apps → Home app.
Source: https://www.axolauncher.com/blog/how-to-change-default-launcher-android
("If you decline that prompt once, it comes back with a 'Don't ask again'
box. Tick it and the launcher can no longer ask, so use the Settings route
below instead.")

**O5 — No account, no sign-in, no email, no phone number — ever, in first run.**
Test: complete first run. Pass if no account/sign-in screen appears at any
point.
Source: https://www.creahive.com/guides/app-onboarding-ux/ ("Sign-in: only
when it is needed" — a launcher never needs it); https://dbggr.dev/articles/user-onboarding-best-practices/
("Let people use the product before they sign up, where the product allows
it.")

**O6 — No permission is requested during first run.**
Test: fresh install → first run. Pass if zero system permission dialogs
appear before the home screen is usable.
Source: https://developer.android.com/training/permissions/requesting?u=http://developer.android.com/guide/topics/renderscript/reference/rs_matrix
("Ask for a permission in context, when the user starts to interact with
the feature that requires it."); https://extensionbooster.net/blog/android-app-onboarding-ux-patterns-reduce-churn-guide/
("Asking for permissions at launch … is one of the fastest ways to destroy
trust on Android.")

**O7 — Every later permission ask is primed, contextual, and cancellable.**
Test: for each permission Marlow can request, trigger it from its feature
(e.g. notifications from the Now Brief toggle). Pass if: a one-sentence
priming screen states what the user gets; the user can cancel it without
the system dialog ever firing; the system dialog fires only after opt-in
on the priming screen.
Source: https://developer.android.com/training/permissions/requesting?u=http://developer.android.com/guide/topics/renderscript/reference/rs_matrix
("Always provide the option to cancel an educational UI flow");
https://extensionbooster.net/blog/android-app-onboarding-ux-patterns-reduce-churn-guide/
(priming structure).

**O8 — A declined permission degrades one feature, never the app.**
Test: deny each permission Marlow can request; use the home screen, drawer,
search and settings. Pass if everything except the dependent feature works,
with no blocking error, no repeated re-prompt, and a one-line explanation
on the dependent toggle.
Source: https://developer.android.com/training/permissions/requesting?u=http://developer.android.com/guide/topics/renderscript/reference/rs_matrix
("If the user denies … gracefully degrade your app so that the user can
continue using your app, possibly by disabling the feature that requires
the permission.")

**O9 — Defaults are shown, not interrogated.**
Test: first run contains no grid-size picker, no theme picker, no toggle
questionnaire. Pass if the home screen simply appears in its default
state (see S22).
Source: https://dbggr.dev/articles/user-onboarding-best-practices/
("Choose defaults carefully, because most people keep them.")

**O10 — No "rate us", no share prompt, no newsletter in first run.**
Test: complete first run and use the launcher for a day. Pass if none of
these appears in the first session.
Source: onboarding principle in §2.2 — friction at the moment of highest
abandonment; no source claims these belong in first run, and the
value-first sources above rule them out at this point.

**O11 — First-run copy is one idea per screen, present tense, sentence case, no jargon.**
Test: read every first-run string aloud. Pass if each screen makes one
point in ≤2 short sentences, with no "launcher", "QuickStep", "grid" or
other internal term unexplained.
Source: Material writing rules as summarised in §1.5; https://www.creahive.com/guides/app-onboarding-ux/
(keep any required flow brief).

### 6.3 Gesture and motion

**G1 — Long-press fires at the system timeout (500 ms default), read from the system, not hard-coded.**
Test: in code review, the long-press detector reads
`ViewConfiguration.getLongPressTimeout()` (or the Compose equivalent that
respects it); grep for hard-coded 300/400/800 ms long-press thresholds.
Pass if none exists.
Source: https://Developer.android.com/reference/kotlin/android/view/ViewConfiguration
(LONG_PRESS_TIMEOUT = 500).

**G2 — Long-press respects the user's Accessibility long-press timeout.**
Test: set Settings → Accessibility → Interaction controls → Long press
timeout to Long; long-press an icon. Pass if the menu/drag waits for the
longer timeout rather than firing at 500 ms.
Source: same ViewConfiguration reference; the framework "respects user
preferences set in Settings > Accessibility > Interaction Controls > Long
Press Timeout" (as documented in the long-press guidance surveyed).

**G3 — Haptic fires at the long-press moment; the icon does not move before it.**
Test: long-press an icon slowly; note when the icon lifts vs when the
haptic fires. Pass if lift and haptic coincide, and no drag is possible
before the haptic.
Source: haptics principles — haptic confirms the state change at the moment
it happens: https://developer.android.com/develop/ui/views/haptics/haptics-principles

**G4 — Swipe between home pages follows the finger, snaps on release, and never scrolls past the last page with a dead bounce.**
Test: drag a page halfway and release (returns), fling (advances one page),
swipe past the last page. Pass if: position tracks the finger 1:1 during
the drag; release past the midpoint commits, before it returns; the last
page resists with a short overshoot and settles, rather than showing blank
space.
Source: Material motion — transitions follow the finger and settle;
easing/duration: https://m3.material.io/styles/motion/easing-and-duration

**G5 — Drawer opens on swipe-up from the dock/home and closes on swipe-down, Back, or tap-outside; it never requires a button.**
Test: swipe up from the dock → drawer; swipe down / Back / tap the scrim
→ home. Pass if all four close paths work and no on-screen button is the
only way to close it.
Source: Pixel behaviour: https://www.androidpolice.com/customize-google-pixel-9-home-screen
(swipe-up drawer); predictive-back dismissal order:
https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back?authuser=0

**G6 — Folder opens as a container transform from its icon.**
Test: tap a folder; watch the open animation. Pass if the folder grows out
of its own icon (shared container, corners interpolating) rather than an
unrelated sheet sliding in from an edge.
Source: Material transition patterns (container transform):
https://m3.material.io/styles/motion/easing-and-duration ; motion
patterns survey: https://github.com/danieldevfull/material-design-3-skill/blob/HEAD/material-design-3/references/motion.md

**G7 — Drawer enter 400 ms emphasized-decelerate; drawer exit 200 ms emphasized-accelerate.**
Test: measure (or read the animation spec for) drawer open/close
durations and curves. Pass if open ≈400 ms decelerate, close ≈200 ms
accelerate (within ±50 ms).
Source: https://m3.material.io/styles/motion/easing-and-duration
("Emphasized decelerate 400ms — Enter the screen; Emphasized accelerate
200ms — Exit the screen").

**G8 — No single-step transition exceeds 500 ms; no micro-interaction exceeds 200 ms.**
Test: list every animation duration in the launcher (press ripple,
toggle, folder, drawer, page, dialog). Pass if none exceeds 500 ms, and
press/toggle/icon-state changes are ≤200 ms.
Source: https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
(duration tokens; long4 = 600 ms is the token ceiling for large surfaces,
and the M3 guidance pairs 500 ms as the begin-and-end default);
https://m3.material.io/styles/motion/easing-and-duration

**G9 — Enter decelerates, exit accelerates — never the reverse, never linear.**
Test: inspect easing curves for enter vs exit transitions. Pass if enters
use a decelerate curve (e.g. emphasized-decelerate 0.05, 0.7, 0.1, 1.0),
exits use an accelerate curve (0.3, 0, 0.8, 0.15), and no one-shot
transition is linear.
Source: https://m3.material.io/styles/motion/easing-and-duration
("Enter the screen — Emphasized decelerate … Exit the screen permanently
— Emphasized accelerate … Transitions without easing look stiff and
mechanical.")

**G10 — Choreography: container first, contents stagger 30–60 ms after.**
Test: open a folder / drawer with several items; watch the order. Pass if
the container lands first and items cascade in with a small stagger,
rather than everything popping at once.
Source: motion choreography survey:
https://github.com/danieldevfull/material-design-3-skill/blob/HEAD/material-design-3/references/motion.md
("Parent leads, children follow … Stagger entering elements by ~30–60ms").

**G11 — Only transform and opacity are animated.**
Test: code review — no animation of width/height/top/left/margin/padding
on the home screen, drawer, or folder paths. Pass if all motion is
translate/scale/alpha.
Source: compositor-thread rule (transform + opacity only), as documented
in the motion-performance research surveyed; consistent with the M3
emphasis on smooth 60 fps transitions.

**G12 — Predictive back: a back swipe on a full-screen launcher surface previews the destination (scale to ~90%, fade-through at 35% progress), and cancelling restores it.**
Test: open a launcher sub-screen / folder / drawer; edge-swipe back
slowly, hold, release without committing. Pass if the surface scales down
toward ~90% as the gesture progresses, the destination fades in at the
commit threshold, and releasing early springs the surface back to 100%
with no state change.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back?authuser=0
("Exit Scale 100% → 90% … Exit Fade 100% → 0% at the 35% progress
threshold … If the user releases the gesture in a non-commit state, the
contents swiftly return and scale back to their original state.")

**G13 — Haptics use predefined constants, never raw `vibrate(long)`.**
Test: grep for `Vibrator.vibrate(`, `createOneShot` for feedback purposes.
Pass if none; all feedback goes through `HapticFeedbackConstants` /
`LocalHapticFeedback` / predefined `VibrationEffect`s.
Source: https://developer.android.com/develop/ui/views/haptics/haptics-principles
("Avoid using the legacy one-shot vibrations … Prioritize predefined
haptic constants and effects").

**G14 — Haptic map (which events get haptics): long-press pickup, drag snap, drop, page snap, folder/drawer threshold, toggle flip, successful confirm — one crisp impulse each.**
Test: perform each event; note haptics. Pass if each fires once, crisply,
at the event moment.
Source: https://developer.android.com/develop/ui/views/haptics/haptics-principles
("To confirm a state change … click feedback … slider snapping into
place"); https://source.android.com/docs/core/interaction/haptics/haptics-ux-foundation?authuser=3
(clear haptics = one effect per one input event).

**G15 — Haptic map (which events get none): icon tap, page swipe, scrolling, typing in search, opening an app.**
Test: perform each. Pass if no haptic fires (or, for scroll, at most the
subtlest tick at a detent — never per-item buzzing).
Source: https://developer.android.com/develop/ui/views/haptics/haptics-principles
("less is more … Haptic effects applied to very frequent events, like
scrolling … should be very subtle"; "Correlate event importance and
frequency with strength").

**G16 — Errors get no haptic.**
Test: trigger a failed action (e.g. drop an icon where it cannot go, a
search with no results). Pass if feedback is visual only.
Source: haptics discipline in the Android UX guidance surveyed — error is
not a confirm; a buzz on failure reads as punishment, and the
"less is more" rule governs.

**G17 — Repeated haptics during a drag are crisp and subtle, never a continuous buzz.**
Test: drag an icon slowly across the grid; feel the snap ticks. Pass if
each tick is discrete and quiet, with silence between ticks — not one long
buzz.
Source: https://source.android.com/docs/core/interaction/haptics/haptics-ux-foundation?authuser=3
("If the individual effect is buzzy rather than crisp, then the intervals
between the repetitions are likely to be wiped out. The outcome is one
long buzz, rather than multiple discrete signals.")

**G18 — Haptics and visuals land on the same frame.**
Test: slow-motion / high-speed observation of a long-press pickup and a
drop. Pass if the haptic coincides with the visual lift / snap, with no
perceptible lag or lead.
Source: https://developer.android.com/develop/ui/views/haptics/haptics-principles
("Make it harmonious or congruent with visual animations … a haptic
feedback that is played out of sync … can be a bit unsettling … the user
may perceive the haptic actuator to be broken.")

**G19 — Keyclick-scale feedback is 10–20 ms, not a 100 ms buzz.**
Test: measure / read the effect used for tap confirmations. Pass if the
impulse is in the 10–20 ms class (predefined CLICK/TICK), not a
duration-based vibration.
Source: https://developer.android.com/develop/ui/views/haptics/haptics-principles
("A good keyclick haptic feedback signal should last between 10 to 20
milliseconds.")

**G20 — Marlow never overrides the system haptics setting.**
Test: turn system haptics off (Settings → Sound & vibration → Haptics);
use Marlow. Pass if no Marlow haptic fires. (Marlow may offer its own
on/off, defaulting to "follow system" — it must not offer "force on".)
Source: https://developer.android.com/design/ui/mobile/guides/patterns/settings
("Don't override settings provided by the system, as they can be personal
accessibility needs.")

**G21 — App launch: the icon scales / the window grows from the icon; it never crossfades from nowhere.**
Test: launch an app from home and from the drawer. Pass if the launch
animation is anchored on the tapped icon (shared-element / container
origin), not a generic fade.
Source: Material container-transform pattern; launcher motion principle
in §3.3 — the icon is the origin of the app.

### 6.4 Accessibility and legibility

**A1 — Every interactive element has a ≥48 × 48 dp touch target.**
Test: with layout bounds on, measure every tappable element (icons,
dots, handles, Settings rows, dialog buttons, search icons). Pass if
every hit area is ≥48 × 48 dp, even where the visible glyph is smaller.
Source:
https://developer.android.com/guide/topics/ui/accessibility/apps?continue=https://developer.android.com/courses/pathways/make-your-android-app-accessible#article-https://developer.android.com/guide/topics/ui/accessibility/apps
("each interactive UI element have a focusable area, or touch target size,
of at least 48dpx48dp. Larger is even better.")

**A2 — Touch targets are ≥8 dp apart where mis-taps are possible.**
Test: measure gaps between adjacent small targets (page dots, drawer
actions). Pass if no two targets are closer than 8 dp edge-to-edge.
Source: Material-derived spacing guidance surveyed (8 dp between targets).

**A3 — Body/label text clears 4.5:1; large text clears 3:1 — on the lightest and busiest bundled wallpapers, light and dark.**
Test: screenshot icon labels, drawer text, search hint and At a Glance
text over the lightest and busiest bundled wallpapers; measure contrast
with a checker. Pass if <18 sp (or bold <14 sp) is ≥4.5:1 and larger text
≥3:1 in every case.
Source: same Android accessibility page as A1 ("If the text is smaller
than 18sp, or if the text is bold and smaller than 14sp, use … at least
4.5:1. For all other text … at least 3:1.").

**A4 — No text sits directly on a wallpaper without a scrim, shadow, or tonal surface behind it.**
Test: inspect every text-over-wallpaper site (icon labels, folder names,
At a Glance, drawer header). Pass if each has a scrim / shadow / surface
that guarantees A3 by construction, not by hoping the wallpaper is dark.
Source: A3's thresholds applied to the launcher's uncontrolled background
(§4.2 derivation).

**A5 — All text uses sp; no text size is hard-coded in dp; font scale is never pinned to 1.**
Test: grep for `fontSize = … .dp`, `fontScale = 1f`, `TextUnit` in dp.
Pass if none.
Source: https://www.thedroidsonroids.com/blog/mobile-app-accessibility-android-guide-part-2
("Use `sp` units so the system font scale applies … Never hard-code `dp`
for text sizes or set `fontScale = 1f` to lock scaling.")

**A6 — Layout survives 200% font scale with no clipped or overlapping text.**
Test: set font scale to 200% (or `@Preview(fontScale = 2f)` + device);
open home, drawer, folder, Settings, dialogs. Pass if no text is cut off,
overlapped, or pushed off-screen, and every screen remains scrollable
where it needs to be.
Source: https://www.thedroidsonroids.com/blog/mobile-app-accessibility-android-guide-part-2
("users should be able to resize text by up to 200 percent … without any
loss of content or functionality"; "Android … has supported non-linear
font scaling to 200% since version 14").

**A7 — Every icon-only control has a contentDescription naming its action, not its appearance.**
Test: turn on TalkBack; navigate every icon-only button. Pass if each
announces an action ("Settings", "Search", "Close") and none announces
"gear icon", "magnifying glass", "Image", or silence.
Source: Android accessibility page (A1) ("include a description that
describes the element's purpose … in the element's `contentDescription`");
https://www.thedroidsonroids.com/blog/mobile-app-accessibility-android-guide-part-2

**A8 — Decorative elements are hidden from the accessibility tree.**
Test: with TalkBack on, swipe through the home screen. Pass if wallpaper
art, decorative dividers, and the page-indicator decoration are skipped,
and TalkBack never announces "Image" for a decoration.
Source: Android accessibility page (A1) ("Use the `hideFromAccessibility`
API to mark purely decorative elements").

**A9 — State is announced, not just shown.**
Test: with TalkBack on, check a toggle, a selected page dot, an expanded
folder, a checked setting. Pass if TalkBack announces the state
("On", "Selected", "Expanded") via `stateDescription` / native control
semantics, not merely a colour change.
Source: Compose accessibility guidance surveyed
(state exposed via `stateDescription`).

**A10 — Every swipe/drag-only action has a TalkBack custom action.**
Test: with TalkBack on, open the custom-actions menu on an app icon, a
folder, and a widget. Pass if move / remove / rename / uninstall
equivalents are available without swiping or dragging.
Source: TalkBack cannot swipe — the custom-actions fallback is the
documented Compose pattern for swipe actions (surveyed in the
accessibility guidance).

**A11 — Focus order is logical and focus returns on dismiss.**
Test: with TalkBack / keyboard navigation, move through home (pages →
dock), drawer (search → list), folder (open → items → close), and a
dialog. Pass if order is reading order, focus enters a popup/dialog on
open, and returns to the invoking element on dismiss.
Source: https://www.thedroidsonroids.com/blog/mobile-app-accessibility-android-guide-part-2
(focus order; WCAG keyboard access).

**A12 — Meaning is never conveyed by colour alone.**
Test: find every colour-only signal (selected page dot, badge, toggle
state, error). Pass if each also has a label, icon, shape, or position
change.
Source: Material 3 accessibility guidance surveyed ("Never convey meaning
through color alone").

**A13 — "Remove animations" is honoured.**
Test: enable Settings → Accessibility → Colour and motion → Remove
animations (or set all three animation scales to Off); use Marlow. Pass
if slides/zooms/parallax are replaced by crossfades or instant changes,
with no large translate/scale motion remaining.
Source: https://github.com/almasumdev/awesome-mobile-accessibility-agent-skills/blob/HEAD/.github/skills/motion/reduce-motion/SKILL.md
("Disable parallax … Replace slide/zoom transitions with crossfade …
Skip large scale/translate transforms; keep opacity changes.")

**A14 — Nothing flashes more than 3 times per second.**
Test: review every animation and loading state. Pass if no element flashes
>3×/second, and no large red flash exists.
Source: same reduce-motion source (WCAG 2.3.1).

**A15 — One-handed: dock, drawer handle, search and back are all reachable / operable with one thumb, and no essential target sits inside a gesture inset.**
Test: hold the phone in one hand; open the drawer, search, and go back.
Pass if all are doable without shifting grip, and no tap target sits fully
inside the edge/bottom gesture insets.
Source: https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back?authuser=0
("Avoid adding touch gestures or drag targets under these gesture areas");
one-handed principle §4.5.

---

## 7. Proposed Marlow settings map (proposal — derived, not Pixel fact)

*This map applies the rules above. It is a proposal for the team to approve,
not a claim about Pixel Launcher's menu. Pixel's real surface is the three
long-press entries in §1.1; everything below organises Marlow's larger
feature set into that shape.*

Top level — **Home settings** (reached by long-press empty space → "Home
settings", and from the app drawer):

1. **Wallpaper & style** — opens the system Wallpaper & style flow (not a
   Marlow re-implementation). Subtext: current wallpaper name / "System".
2. **Home screen** — Grid ("5 × 4"), Icon labels ("On"), At a Glance
   ("On"), Suggestions ("On"). Sub-screen.
3. **App drawer** — Layout ("Vertical"), Search ("On"), Suggestions in
   drawer ("On"). Sub-screen.
4. **Dock** — Dock ("On"), Folders in dock ("Off"). Sub-screen. [PROPOSAL]
5. **Gestures** — Swipe up for drawer ("On"), Double-tap to lock ("Off"),
   Home button action ("Home screen"). Sub-screen.
6. **Appearance** — Theme ("System default"), Accent ("Phosphor blue"),
   Themed icons ("Off"). Sub-screen. (Theme default "Stock" per project
   decision 2026-11-26; Material You one tap away — the *label* must say
   that in plain words, not in a codename.)
7. **About** — status lines only: "Home app: Marlow", "Version …". No
   toggles. Bottom of the list.

Search sits at the top of Home settings (S20). "Advanced" does not exist
until some screen genuinely has ≥3 rarely-changed items to hide (S9).
Backup / Restore lives as two verb rows at the bottom of the top level
("Back up home screen", "Restore backup") — actions, not settings (see
§1.4). Nothing else is added without a rule justifying its place.

---

## 8. EXPANSION — Insider-level working knowledge, from real sources only

*Added per the parent agent's scope expansion (Robert's correction: he is
NOT asking for fabricated credentials — he wants the research expanded so
the team has the same information someone who worked at those companies
would realistically have, to the extent public sources allow). Everything
below is from public talks, documentation, and published engineering
writing, cited. Where a speaker's name could not be verified from the
sources read, it is marked "not verified" — it is not guessed. No claim in
this section is based on personal employment or insider access.*

### 8.1 The talks — what was actually said, and where

| # | Talk / session | Year / event | What it establishes for Marlow | Source |
|---|---|---|---|---|
| T1 | "What's new in notifications, launcher icons, and shortcuts" (Google I/O '17) | 2017, Google I/O | The Android O launcher contract: adaptive icons, pinned shortcuts, and app shortcuts (long-press menus) as a *system* API, not a launcher invention. A launcher must render what apps publish — shortcuts, icon shapes — not invent its own shortcut format. The talk is listed as a 40-minute I/O '17 session by the SystemUI team, covering "new APIs and features for the app launcher, lockscreen, and notification manager in the Android O release". Speaker names not verified from the sources read. | https://www.classcentral.com/course/youtube-what-s-new-in-notifications-launcher-icons-and-shortcuts-google-i-o-17-211952 |
| T2 | "Dark theme & gesture navigation" | 2019, Google I/O '19 | The Android 10 gesture model as shipped: swipe up = home, swipe up-and-hold = Overview, swipe from either edge = back, swipe across the bottom pill = switch apps; diagonal corner swipe = Assistant. Also the drawer-peek compromise (tap-and-hold the edge to peek a navigation drawer, because the back edge-swipe collides with it). For Marlow: the home screen must not put its own edge gestures where the system back gesture lives, and a drawer inside Marlow must use the peek pattern, not a competing edge swipe. Listed as a video resource on the gesture-navigation documentation page. | https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav?authuser=5 |
| T3 | "What's new in Android" | 2021, Google I/O | Speakers named in the session listing: **Chet Haase, Dan Sandler, and Romain Guy**. Android 12 as "one of the biggest design changes ever": Material You colour extraction from wallpaper, refreshed widgets, new splash-screen / launch animations, stretch overscroll, audio-coupled haptic playback, and a new ripple/edge effect. For Marlow: wallpaper-driven colour is a *system* behaviour to follow, not a theme to re-implement; launch animations and overscroll are part of what "stock" feels like. | https://9to5google.com/2021/05/18/android-12-google-io/ ; video: https://www.youtube.com/embed/D2cU_itNDAI?feature=oembed&rel=0&enablejsapi=1 ; summary: https://developers.googleblog.com/whats-new-for-android-developers-at-google-io/ |
| T4 | "Refreshing widgets" | 2021, Google I/O | Widgets get a new look in Android 12 and a reworked Widgets API: rounded corners (mandatory — widgets that ignore them look out of place), dynamic colour / Material You theming for third-party widgets, interactive controls (checkboxes, radio buttons, switches) usable without opening the app, responsive previews in the picker, exact sizes and responsive layouts, a description for discoverability, and a reconfigure affordance so a placed widget can be edited without deleting it. Also: widget content surfaced to Assistant via the Capabilities API. For Marlow: the widget picker must show responsive previews and descriptions; placed widgets must be reconfigurable in place; widget corners must be rounded to the system radius. Speaker names not verified from the sources read. | https://9to5google.com/2021/05/18/android-12-google-io/ ; https://9to5google.com/2021/04/28/google-io-2021-sessions/ ; video: https://www.youtube.com/embed/cHv84zXZf6E?feature=oembed&rel=0&enablejsapi=1 ; write-up: https://www.XDA-Developers.com/google-android-12-widgets-overhaul/ |
| T5 | "Back to the basics of System Back" | 2022, Google I/O (May 12) | The predictive-back model: an *ahead-of-time* model in which the app tells the system in advance whether it handles back, so the system can show a preview of the destination before the gesture completes. Opt-in in Android 13, default in Android 14 (target SDK 34). Migration: replace `KEYCODE_BACK` / `onBackPressed()` with `OnBackPressedCallback` (AndroidX Activity 1.6.0+) or `OnBackInvokedCallback`; Compose gets `BackHandler` / `PredictiveBackHandler`. The back-to-home preview shrinks the app to reveal the wallpaper/home — the animation a launcher owner sees every time. Speaker names not verified from the sources read. Video linked from a session recap. | https://www.youtube.com/watch?v=Elpqr5xpLxQ ; https://9to5google.com/2022/05/11/android-13-back-gesture-predictable/ ; https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture?authuser=5 ; https://www.esper.io/blog/android-dessert-bites-23-android-13-predictive-back-navigation-37581549 |
| T6 | "What's new in Android" (Android 13 segment) | 2022, Google I/O | Predictive back was first shown here (around 10:12 in the keynote video, per 9to5Google): if going back would close the app, the app shrinks to reveal the wallpaper, "acting as a hint that you'll end up back at your home screen", giving the user time to cancel. That single behaviour is the whole point of predictive back for a launcher: the home screen *is* the preview. | https://9to5google.com/2022/05/11/android-13-back-gesture-predictable/ |
| T7 | Large-screens sessions: "Building adaptive Android apps", "Designing adaptive apps", "Building UI with the Material 3 adaptive library" | 2024, Google I/O | The adaptive definition, from the I/O 2024 recap by Fahd Imtiaz, Product Manager, Android Developer: adaptive apps "take advantage of the full screen size … adjust layout based on conditions … changes to the size of the window, device posture or font size" and "dynamically adjust their layouts by swapping components, showing, or hiding content based on the available window size, compared to simply stretching UI elements". Recommended: WindowSizeClasses as the breakpoints; Compose adaptive layout libraries (list-detail, supporting pane, pane expansion — Google Calendar's pane expansion is the cited example). For Marlow: a foldable/tablet home screen is not a stretched phone grid; it swaps components (wider grid, two-pane settings via list-detail). | https://android-developers.googleblog.com/2024/05/get-big-picture-with-large-screens-at-google-io-24.html |
| T8 | Adaptive sessions + "Building Premium Android Experiences" | 2025–2026, Google I/O | 2025: adaptive is "essential", not optional; 500M+ large-screen devices; Android 16 desktop windowing and connected displays. 2026 (recap by Ataul Munim, Developer Relations Engineer): Android 17 removes the opt-out for orientation/resizability restrictions on large screens (sw > 600 dp) — apps *must* adapt; Compose is now the standard ("Compose-first", Views in maintenance mode); Navigation 3; and the unified widget model below (T9). For Marlow: the foldable/tablet rows in Settings must be device-gated (as the 2026-11-26 audit already decided), and the layout must be window-size-class driven, not device-model driven. | http://android-developers.googleblog.com/2025/05/adaptiveapps-io25.html ; https://developer.android.com/blog/posts/building-premium-android-experiences-at-google-i-o-26 ; https://developer.android.com/blog/posts/17-things-to-know-for-android-developers-at-google-i-o |
| T9 | Unified widgets: Jetpack Glance + RemoteCompose | 2026, Google I/O | One Compose-based model (Jetpack Glance) for widgets on phone, Wear (Wear Widgets, formerly Tiles) and cars; RemoteCompose as the engine underneath, giving widgets snap-scroll, expressive components, particle effects, smooth fade-and-morph resizing, and dynamic theming — natively on Android 16+, with safe static fallbacks on Android 15 and below. The same journey (flight status on the car dashboard, gate change on the watch, boarding pass on the phone widget) is the cited example. For Marlow: the widget *host* must expect widgets that animate and resize smoothly, and must not assume a widget is a static bitmap; the picker and the resize interaction are part of the stock feel. | https://developer.android.com/blog/posts/building-premium-android-experiences-at-google-i-o-26 ; https://developer.android.com/blog/posts/17-things-to-know-for-android-developers-at-google-i-o ; https://developer.android.com/blog/posts/building-for-the-intelligence-system-on-android ; https://www.androidauthority.com/android-widgets-overhaul-remote-compose-3670122/ |
| T10 | Material 3 Expressive (The Android Show: I/O Edition, May 2025; in Pixel phones with Android 16 QPR1, Sept 2025) | 2025 | The biggest Material update since Material You: spring-based motion physics replace fixed easing curves *for components* (stiffness, damping, slight overshoot and settle); legacy easing/duration remains for *transitions*. Bolder shapes, shape morphing, emphasized type, background blur on system surfaces, notification swipe-away with elastic reaction + a tiny haptic bump, resizable Quick Settings tiles, Live Updates. Google's research claim, as reported: people spotted key UI elements up to 4× faster on expressive screens. For Marlow: use `MotionScheme` springs for component motion (press, morph, dismiss), keep emphasized easing for screen transitions — and do not apply Expressive springs to everything, or the home screen becomes a toy. | https://m3.material.io/styles/motion/overview/specs ; https://m3.material.io/styles/motion/easing-and-duration ; https://techstuffs.net/android-16-material-3-expressive/ |

**Talks searched for but not verified as standalone launcher talks.** No
standalone Google I/O talk titled for "the launcher" itself was found in the
sources read — the launcher is covered *through* the talks above (shortcuts,
widgets, gestures, predictive back, adaptive layouts) and through the
Pixel Launcher documentation, not as its own session. That absence is itself
a finding: Google treats the launcher as the *integration point* of system
behaviours, not as a feature with its own talk. Do not cite a launcher talk
that was not found.

### 8.2 Design-engineering writing on launcher motion — what the builders actually say

**The QuickStep boundary (the single most important launcher-motion fact).**
With Android 9 Pie, Google moved the Recents/Overview screen out of
SystemUI and into Launcher3 (the open-source launcher most stock launchers
are forked from), as the QuickStep component — because gesture navigation
needed app transitions to look seamless and the app list to be reachable
from Recents. Because QuickStep is privileged, only the preinstalled
launcher can be the Recents provider; a third-party launcher relies on the
stock launcher for gestures and Recents, and the result "can be a bit
janky, with transitions that don't look fluid and seamless". On Pixel
phones there is an API third-party launchers can use: QuickStep sends the
launcher an intent and a callback on swipe-to-home, and the launcher hints
how the window should animate as it minimises onto an app icon — Nova
Launcher (v7 builds) and Niagara Launcher use it, and the comparison in
the XDA piece shows the animation landing on the icon on Pixel vs not on
other devices. Source:
https://www.xda-developers.com/third-party-launchers-animation-pixel-phones/
The 2026 How-To Geek piece reaches the same conclusion from the user side:
"Custom Android launchers can't handle navigation gestures … QuickStep …
is seamlessly integrated with stock launchers … it treats third-party
launchers like regular apps" — and even in 2026, on a Pixel 10 Pro, small
hiccups remain. Source:
https://www.howtogeek.com/gestures-dont-work-well-with-android-launchers-and-its-googles-fault/
Marlow consequence: Marlow's home-transition quality is *bounded* by this
boundary. The team should test the swipe-home animation on the Pixel 10a
specifically (the project's device gate), compare icon-anchored minimise
vs fade, and record which it is — not tune a duration and declare it fixed.

**The predictive-back animation was prototyped against the launcher.**
Mishaal Rahman's Esper piece (pre-I/O 2022) reports, with help from Kevin
Barry (developer of Nova Launcher), that Pixel Launcher's Quickstep module
contained code hinting at the new back-to-home transition: the app window
scales and follows the finger as it swipes inward, and on release past the
threshold the Android 12L animation plays — "gracefully animates the icon
towards its location on the home screen or app drawer". Source:
https://www.esper.io/blog/android-dessert-bites-23-android-13-predictive-back-navigation-37581549
That is the motion Marlow's own back-to-home preview must match: scale +
follow + icon-anchored settle, not a fade.

**Motion is choreography, not duration.** The M3 motion references
converge on the same choreography rules (§3.3): parent leads, children
follow with a 30–60 ms stagger; enter slow/decelerated, exit fast/
accelerated; bigger elements get more pronounced motion; follow-through —
content fades in 50–100 ms after its container lands. Source:
https://github.com/danieldevfull/material-design-3-skill/blob/HEAD/material-design-3/references/motion.md
(and the M3 pages in the source list). For a launcher, the visible
application is the drawer and the folder: the sheet/container is the
parent, the icons are the children, and the stagger is what makes a grid
feel like it *arrives* rather than *appears*.

**M3 Expressive's motion lesson for a launcher.** Springs feel alive
because they don't stop dead — they slightly overshoot and settle. But the
Expressive guidance is explicit that the legacy easing/duration system is
still the one for transitions, and springs are for components. A launcher
that springs every page swipe will feel drunk; a launcher that springs a
folder open, a toggle, and a dismiss will feel alive. One or two hero
moments per product, not per screen — the Expressive design rules surveyed
put it exactly that way ("Use one or two per product, not per screen; more
than that overwhelms"). Source:
https://m3.material.io/styles/motion/overview/specs

### 8.3 Design-engineering writing on haptics — what the builders actually say

**Google's haptics team, in their own words (blog.google, 15 Nov 2023).**
Lead haptics scientist Hong Tan and lead sensor engineer Debanjan
Mukherjee describe the shift from the old 200–300 Hz "soprano" buzz to
rounder, click-like and gentle variants, tested in the team's own playground
app with named feels — "click" (very short), "tick" (less intense),
"low tick" (softer, lower frequency, more organic), plus "thud" and "quick
fall". "Haptics accomplishes so much," Tan says. "They alert, inform and
delight!" The industrial-design side (Stefan Reichert on the Chromecast
remote; Rhea Jeong on the Nest Doorbell) adds the physical discipline:
a good click is a balance of force and travel time — not mushy (no
resistance feels cheap), not tiring (too much resistance), tested with
prototypes and with dexterity / visual-impairment access in mind ("You can
easily identify it even if you're visually impaired" — the doorbell's top
button is a different colour *and* a different dome shape). "There's no
golden recipe," Jeong says. "We just want to make sure that every button we
put out into the world is designed with care." Source:
https://blog.google/products-and-platforms/devices/pixel/google-haptic-design/
Marlow consequence: haptics are tuned by *feel words* (click / tick / low
tick), not by milliseconds in a vibrate call — and the test is a human
hand, not a log line. The team's haptic review should be done on the Pixel
10a, naming each effect in those words.

**The framework view (AOSP + Android Developers).** The UX foundation
paper frames the whole programme as replacing buzzy vibration with clear
haptics, then exploring rich haptics — and it is explicit that repeated
effects during a swipe/drag must be "crisp and subtle", using `CLOCK_TICK`
and `TEXT_HANDLE_MOVE`, or composed `PRIMITIVE_CLICK` / `PRIMITIVE_TICK`
sequences, so the result is discrete ticks and not one long buzz. Source:
https://source.android.com/docs/core/interaction/haptics/haptics-ux-foundation?authuser=3
The developer principles add the intensity discipline (§3.4) and the
10–20 ms keyclick figure. Source:
https://developer.android.com/develop/ui/views/haptics/haptics-principles

**The 2025 state of the art (World Haptics Conference tutorial).**
"Android Haptics: API and Implementation on Pixel", a tutorial at World
Haptics Conference 2025 (Suwon, 8 July 2025) by Ahmad Khalil and Nathan
Kulczak (software engineers at Google, per the tutorial slides) and Hong
Tan (research scientist, Google, haptics research team), covers human
haptic perception, the Android haptics APIs, and the Pixel implementation —
including Pixel's factory calibration of the actuator, waveform-analysis
validation tests, and the Android 16 PWLE (piecewise-linear envelope)
APIs (`BasicEnvelopeBuilder` for a hardware-agnostic approach,
`WaveformEnvelopeBuilder` for the advanced one) that let designers specify
intensity/sharpness over time instead of a single buzz. Source:
https://2025.worldhaptics.org/wp-content/uploads/2025/07/WHC2025_tutorial1.pdf
Marlow consequence: on Android 16+ devices, a custom effect (if one is
ever needed — the default should still be the predefined constants) should
be an envelope, not a one-shot; and any custom effect must degrade
gracefully on devices whose actuators cannot render it, because the
predefined constants exist precisely to provide that fallback.

**The multisensory rule.** Haptics are co-designed with visual and audio
effects — "Make it harmonious or congruent" — and the Android Developers
haptics hub points to the Multisensory Design System (MSDS) for
audio-haptic feedback. A haptic that fires a frame late reads as a broken
actuator. Source:
https://developer.android.com/develop/ui/views/haptics/haptics-principles ;
https://developer.android.com/develop/ui/views/haptics?hl=en

### 8.4 Checkable rules from the expansion (X-rules)

**X1 — Marlow does not intercept or re-implement the system swipe-up-and-hold (Overview) gesture.**
Test: swipe up and hold from an app while Marlow is home. Pass if the
system Overview appears; Marlow shows no custom Recents screen and no
custom animation over it.
Source: https://www.xda-developers.com/third-party-launchers-animation-pixel-phones/
(QuickStep owns Recents; only the preinstalled launcher is the provider).

**X2 — The swipe-home animation is icon-anchored on the Pixel 10a, and the team has recorded which it is.**
Test: on the Pixel 10a, swipe home from an app and watch the window.
Pass if the window shrinks toward its home-screen/drawer icon (not a fade
or a slide), and the result is written in the build log as "icon-anchored"
or "not icon-anchored — QuickStep boundary", with no claim of a fix that
was not observed.
Source: https://www.esper.io/blog/android-dessert-bites-23-android-13-predictive-back-navigation-37581549 ;
https://www.xda-developers.com/third-party-launchers-animation-pixel-phones/

**X3 — No Marlow edge gesture competes with system Back; a drawer inside Marlow uses the peek pattern.**
Test: edge-swipe from the left/right inside any Marlow surface. Pass if
system Back wins; if Marlow has a side drawer, tap-and-hold on the edge
peeks it (Android 10 drawer-peek behaviour) rather than a swipe opening it.
Source: https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav?authuser=5
(T2; drawer peek).

**X4 — App shortcuts are rendered from what apps publish; long-press shows shortcuts, App info, and Widgets — in that stock order.**
Test: long-press an app with shortcuts and widgets. Pass if the menu shows
the app's own shortcuts (not Marlow-invented ones), App info, and a
Widgets entry; dragging a shortcut to home pins it.
Source: T1 (I/O '17 shortcuts contract); Pixel behaviour surveyed in §3.1.

**X5 — The widget picker shows responsive previews and descriptions; a placed widget can be reconfigured without deleting it.**
Test: open the widget picker; check each entry has a preview at more than
one size and a description; place a widget, long-press it, choose
Reconfigure. Pass if reconfiguration edits it in place.
Source: T4 ("Refreshing widgets", I/O 2021):
https://9to5google.com/2021/05/18/android-12-google-io/ ;
https://www.XDA-Developers.com/google-android-12-widgets-overhaul/

**X6 — Widget corners are rounded to the system radius; widgets follow dynamic colour.**
Test: place third-party widgets on light and dark wallpapers. Pass if no
widget shows square corners against a rounded system, and widget colours
follow the wallpaper-derived palette.
Source: T4 (same sources as X5).

**X7 — Springs are used for components (press, morph, dismiss); emphasized easing is used for transitions. Never the reverse.**
Test: code review of the motion table. Pass if component motion uses a
spring / `MotionScheme` and screen transitions use the §3.3 easing pairs.
Source: https://m3.material.io/styles/motion/overview/specs (T10).

**X8 — At most two "hero" motion moments in the whole launcher.**
Test: list the moments designed to delight (e.g. folder open, first-run
welcome). Pass if ≤2, and no other screen has a hero animation.
Source: M3 Expressive design rules surveyed ("one or two per product, not
per screen").

**X9 — On Android 16+, any custom haptic is an envelope (PWLE), not a one-shot; on older devices it falls back to a predefined constant.**
Test: code review of any custom effect. Pass if it uses
`BasicEnvelopeBuilder` / `WaveformEnvelopeBuilder` (or the Compose
equivalent) with a predefined-constant fallback, and no `vibrate(long)`
exists.
Source: https://2025.worldhaptics.org/wp-content/uploads/2025/07/WHC2025_tutorial1.pdf ;
https://developer.android.com/develop/ui/views/haptics/haptics-principles

**X10 — Haptic review is done by hand on the Pixel 10a, naming each effect click / tick / low tick.**
Test: the build log records a haptic pass on the device, with each event
named in feel words. Pass if the record exists and no event is described
only by a millisecond number.
Source: https://blog.google/products-and-platforms/devices/pixel/google-haptic-design/
(Hong Tan / Debanjan Mukherjee: click, tick, low tick, thud, quick fall).

**X11 — Foldable / tablet layout swaps components by window size class; it is not a stretched phone grid.**
Test: open Marlow on a foldable unfolded / tablet (or an emulator at
Expanded width). Pass if the grid widens, Settings uses a list-detail
layout, and no phone-width column is simply stretched.
Source: T7/T8:
https://android-developers.googleblog.com/2024/05/get-big-picture-with-large-screens-at-google-io-24.html ;
http://android-developers.googleblog.com/2025/05/adaptiveapps-io25.html

**X12 — Marlow's home screen honours the launcher's role as the predictive-back preview.**
Test: from an app, edge-swipe back slowly. Pass if the preview that
appears is the actual home screen (wallpaper, icons, dock) shrinking into
view — not a blank surface or a placeholder.
Source: T5/T6:
https://9to5google.com/2022/05/11/android-13-back-gesture-predictable/ ;
https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back?authuser=0

---

## 9. The 10 rules most likely to matter for a stock-feeling launcher

1. **S22 — Stock defaults.** Fresh install looks like a Pixel: labels on,
   dots on, suggestions on, themed icons off, swipe-up drawer on, portrait
   locked. If the first screen doesn't look stock, no settings page will
   save it.
2. **S1 + S2 — 5–7 top-level sections, ≤15 rows per screen.** The difference
   between Pixel and Nova is count, not taste. Count it.
3. **S5 — Every row shows its value.** A settings screen you can read at a
   glance is a settings screen you trust; a screen of descriptions is a
   manual.
4. **O3 + O4 — Set-as-default via the system prompt, once, with a Settings
   fallback after a decline.** The one first-run question, done the way
   Android does it, and never nagged.
5. **O6 + O8 — No permission at first run; a decline disables one feature,
   never the app.** Trust is built by what you *don't* ask for.
6. **G1 + G2 — Long-press at the system 500 ms, respecting the user's
   Accessibility timeout.** Timing is platform muscle memory, not a tuning
   knob.
7. **G7 + G9 — Drawer 400 ms in / 200 ms out, decelerate in, accelerate
   out.** The single most-repeated transition in a launcher; get its pair
   right and the whole app feels faster.
8. **G14 + G15 — Haptics only where they confirm something, never on tap,
   scroll, or error.** One crisp click at pickup/drop; silence everywhere
   else. This is the cheapest way to stop feeling cheap.
9. **A1 + A3 — 48 dp targets, 4.5:1 text on the worst wallpaper.** The two
   numbers a device test can falsify in a minute — and the two a user feels
   every time they miss a dot or can't read a label on a bright photo.
10. **X2 — The swipe-home animation is icon-anchored on the Pixel 10a, and
    the team has honestly recorded whether it is.** This is the transition
    the user sees dozens of times a day, it is bounded by the QuickStep
    boundary rather than by Marlow's code, and pretending it is fixed when
    it isn't is exactly how a launcher ends up feeling "terrible" on the
    device while looking fine in a build log.

---

*End of track 4. No code was changed. No credentials were claimed. Every
rule above can be checked against the app; every source above can be
opened and read.*
