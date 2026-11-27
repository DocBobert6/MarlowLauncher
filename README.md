# Marlow Launcher

A home-only Android launcher — Pixel-based grid home, iOS 26 elements
where logical, no Liquid Glass (custom blur glass / Opaque only), warm
CRT-accurate colours with a modern look, blue phosphor-glow default
accent. No lock screen. No root. Lightweight is a goal, not a
requirement.

- **Plan:** `~/workspace/your_files/android-launcher-plan/android-launcher-plan.pdf`
  (33 pages, audited 2026-11-26: 93 approved / 77 denied or removed, of 170)
- **Decisions:** `~/workspace/launcher-plan/feature-decisions.md`
- **Build log:** `BUILD_LOG.md` (phase order, CI runs, what is verified
  vs what is device-gated, open questions)
- **Package:** `app.launcher`. **Name:** Marlow Launcher (decided
  2026-11-26). **Version:** 0.1.0-dev.
- **Repo:** https://github.com/DocBobert6/MarlowLauncher (public)

## Status

Phases 0–5 are complete and the finish job (2026-11-26) closed the
audit gaps: page-index handling, dock add, pinned shortcuts, drawer
long-press, desktop-lock guards, grid spacing, At a Glance weather
(Open-Meteo), Ambient theme, Now Brief morning delivery, and the
app icon revised for real launcher size (see `BUILD_LOG.md`).

CI is green (run 14, 2026-11-26): the 3 gate tests, the Manifest
guard, and the 255-register dex guard all pass, and the first debug
APK was verified from a download (61,835,829 bytes, SHA-256
`f4cff223…`). The app runs on a device (confirmed by Robert), but
the full device test — the picture, the gestures, the icon at
launcher size — is still his call.

## Building

### Locally (workspace) — kotlinc-check only

Local Gradle builds are **impossible** in this sandbox: the
`other_tcp: Deny` permission intercepts TCP from Java processes, so
the Gradle daemon handshake fails (proven repeatedly — do not
attempt a bypass). CI is the build authority. The local gate is the
Kotlin type check, which needs `java` on PATH first:

```sh
export PATH=$HOME/workspace/tools/jdk/jdk-21.0.12.1+1/bin:$PATH
./tools/kotlinc-check.sh
```

It type-checks every source file against the offline Compose 1.7.6
classpath. Note the version skew: CI builds with Compose BOM
2025.01.00 (UI 1.8.x), so code must pass **both** — e.g.
`LayoutCoordinates.boundsInParent()` does not exist in 1.7.6.

Icons are verified separately (never on the 512px square alone):

```sh
cd icon-options && python3 verify_icon.py
```

That renders the launcher vector at 512px against the design and at
48px / 96px under 66dp and 72dp circular masks, and measures the
light spill and the dock pill at 48px masked.

### On GitHub (CI)

`.github/workflows/android.yml` runs on push to `main`: the Manifest
guard, unit tests (including the Robolectric startup gate and the
WeatherProvider parsing tests), the debug build, the 255-register
dex guard, and uploads the `launcher-debug-apk` artifact. Public
repos get free unlimited Actions minutes, so CI runs on every push.

Pushes go through the git-database API (there is no git remote —
plain `git push` gets a 401), via `gh_api.py push-tree` from a
`git archive HEAD` staging copy. See `BUILD_LOG.md` for the exact
commands. Docs-only pushes use `[skip ci]` in the message.

## Standing rules

- Build only approved plan items. Never re-decide a feature.
- Fixes standalone; never trade features for fixes.
- Never claim a build is verified unless it actually compiled —
  verify every release from your own downloaded APK (file type +
  exact byte size + version + signature) before announcing it.
- Keep the 255-register dex guard, the Robolectric startup gate and
  the Manifest guard permanently; never remove or weaken them to get
  a build green.
- No ROMs, no copyrighted assets, no secrets, no keystores in the repo.
- Icons: verify at real launcher size under the circular mask.
  Never approve an icon on the 512px square alone.
